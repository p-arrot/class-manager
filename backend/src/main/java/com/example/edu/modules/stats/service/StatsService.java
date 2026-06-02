package com.example.edu.modules.stats.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.course.entity.Lesson;
import com.example.edu.modules.course.mapper.LessonMapper;
import com.example.edu.modules.evaluation.entity.Evaluation;
import com.example.edu.modules.evaluation.mapper.EvaluationMapper;
import com.example.edu.modules.exam.entity.Exam;
import com.example.edu.modules.exam.entity.ExamSubmission;
import com.example.edu.modules.exam.mapper.ExamMapper;
import com.example.edu.modules.exam.mapper.ExamSubmissionMapper;
import com.example.edu.modules.project.entity.Project;
import com.example.edu.modules.project.entity.ProjectScore;
import com.example.edu.modules.project.mapper.ProjectMapper;
import com.example.edu.modules.project.mapper.ProjectScoreMapper;
import com.example.edu.modules.task.entity.Submission;
import com.example.edu.modules.task.entity.Task;
import com.example.edu.modules.task.mapper.SubmissionMapper;
import com.example.edu.modules.task.mapper.TaskMapper;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import com.example.edu.modules.classes.entity.SchoolClass;
import com.example.edu.modules.classes.mapper.SchoolClassMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private static final Map<String, Integer> GS = Map.of("A", 100, "B", 80, "C", 60, "D", 40, "E", 20, "F", 0);
    private static final List<String> DIMS = List.of("AWARENESS", "COMPUTING", "DIGITAL_LEARNING", "RESPONSIBILITY");

    private final EvaluationMapper evaluationMapper;
    private final ExamSubmissionMapper examSubmissionMapper;
    private final ExamMapper examMapper;
    private final ProjectScoreMapper projectScoreMapper;
    private final ProjectMapper projectMapper;
    private final SubmissionMapper submissionMapper;
    private final TaskMapper taskMapper;
    private final LessonMapper lessonMapper;
    private final UserMapper userMapper;
    private final SchoolClassMapper schoolClassMapper;
    private final AuditLogService auditLogService;

    public record GradeRow(
            String school, String className, String studentNo, String studentName,
            Integer awareness, Integer computing, Integer digitalLearn, Integer responsibility,
            Double processScore, Double examScore, Double projectScore,
            Double resultScore, Double totalScore, String totalGrade, String remark) {}

    public List<GradeRow> calculateSemesterGrades(Long semesterId) {
        // 1. Collect all tasks in this semester's lessons
        List<Lesson> lessons = lessonMapper.selectList(
                new LambdaQueryWrapper<Lesson>().eq(Lesson::getSemesterId, semesterId));
        if (lessons.isEmpty()) return List.of();
        List<Long> lessonIds = lessons.stream().map(Lesson::getId).toList();
        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>().in(Task::getLessonId, lessonIds));

        // 2. Get all students via submissions
        List<Submission> allSubs = new ArrayList<>();
        for (Task t : tasks) {
            allSubs.addAll(submissionMapper.selectList(
                    new LambdaQueryWrapper<Submission>().eq(Submission::getTaskId, t.getId())));
        }
        Set<Long> studentIds = allSubs.stream().map(Submission::getStudentId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, SchoolClass> classMap = schoolClassMapper.selectBatchIds(
                userMap.values().stream().map(User::getClassId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(SchoolClass::getId, c -> c));

        // 3. Get evaluations
        Set<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toSet());
        List<Evaluation> evals = evaluationMapper.selectList(
                new LambdaQueryWrapper<Evaluation>()
                        .in(Evaluation::getSourceId, allSubs.stream().map(Submission::getId).collect(Collectors.toList()))
                        .eq(Evaluation::getIsSpecial, 0));
        Map<Long, List<Evaluation>> evalsBySubmission = evals.stream()
                .collect(Collectors.groupingBy(Evaluation::getSourceId));

        // 4. Get exams
        List<Exam> exams = examMapper.selectList(
                new LambdaQueryWrapper<Exam>().eq(Exam::getSemesterId, semesterId));
        Map<Long, List<ExamSubmission>> examSubsByStudent = new HashMap<>();
        for (Exam exam : exams) {
            List<ExamSubmission> examSubs = examSubmissionMapper.selectList(
                    new LambdaQueryWrapper<ExamSubmission>().eq(ExamSubmission::getExamId, exam.getId()));
            for (ExamSubmission es : examSubs) {
                examSubsByStudent.computeIfAbsent(es.getStudentId(), k -> new ArrayList<>()).add(es);
            }
        }

        // 5. Get projects
        List<Project> projects = projectMapper.selectList(
                new LambdaQueryWrapper<Project>().eq(Project::getSemesterId, semesterId));
        Map<Long, List<ProjectScore>> scoresByStudent = new HashMap<>();
        for (Project p : projects) {
            List<ProjectScore> scores = projectScoreMapper.selectList(
                    new LambdaQueryWrapper<ProjectScore>().eq(ProjectScore::getProjectId, p.getId())
                            .eq(ProjectScore::getIsSpecial, 0));
            for (ProjectScore ps : scores) {
                scoresByStudent.computeIfAbsent(ps.getStudentId(), k -> new ArrayList<>()).add(ps);
            }
        }

        // 6. Calculate per student
        List<GradeRow> rows = new ArrayList<>();
        for (Long sid : studentIds) {
            User u = userMap.get(sid);
            if (u == null) continue;
            SchoolClass sc = u.getClassId() != null ? classMap.get(u.getClassId()) : null;

            // Process evaluation: weighted avg of worksheet(1.0) + artifact(1.5)
            double processWeightedSum = 0;
            double processWeightTotal = 0;
            Map<String, List<Integer>> dimScores = new LinkedHashMap<>();
            for (String d : DIMS) dimScores.put(d, new ArrayList<>());

            for (Submission sub : allSubs.stream().filter(s -> s.getStudentId().equals(sid)).toList()) {
                Task t = tasks.stream().filter(tk -> tk.getId().equals(sub.getTaskId())).findFirst().orElse(null);
                if (t == null || "special".equals(sub.getStatus())) continue;
                List<Evaluation> subEvals = evalsBySubmission.getOrDefault(sub.getId(), List.of());
                if (subEvals.isEmpty()) {
                    // Auto F for unsubmitted
                    double score = "graded".equals(sub.getStatus()) ? 0 : 0;
                    if ("graded".equals(sub.getStatus())) {
                        for (String d : DIMS) dimScores.get(d).add(0);
                    }
                    double w = "artifact".equals(t.getType()) ? 1.5 : 1.0;
                    processWeightedSum += score * w;
                    processWeightTotal += w;
                } else {
                    double avg = subEvals.stream().mapToInt(e -> GS.getOrDefault(e.getGrade(), 0)).average().orElse(0);
                    for (Evaluation e : subEvals) {
                        dimScores.get(e.getDimension()).add(GS.getOrDefault(e.getGrade(), 0));
                    }
                    double w = "artifact".equals(t.getType()) ? 1.5 : 1.0;
                    processWeightedSum += avg * w;
                    processWeightTotal += w;
                }
            }
            Double processScore = processWeightTotal > 0 ?
                    round(processWeightedSum / processWeightTotal) : null;

            // Dimension averages
            Integer awareness = avgOrNull(dimScores.get("AWARENESS"));
            Integer computing = avgOrNull(dimScores.get("COMPUTING"));
            Integer digitalLearn = avgOrNull(dimScores.get("DIGITAL_LEARNING"));
            Integer responsibility = avgOrNull(dimScores.get("RESPONSIBILITY"));

            // Exam score: weighted average
            double examWeightedSum = 0, examWeightTotal = 0;
            List<ExamSubmission> studentExamSubs = examSubsByStudent.getOrDefault(sid, List.of());
            for (ExamSubmission es : studentExamSubs) {
                Exam exam = exams.stream().filter(e -> e.getId().equals(es.getExamId())).findFirst().orElse(null);
                if (exam == null || es.getScore() == null) continue;
                double w = exam.getWeight() != null ? exam.getWeight().doubleValue() : 1.0;
                examWeightedSum += es.getScore() * w;
                examWeightTotal += w;
            }
            Double examScore = examWeightTotal > 0 ? round(examWeightedSum / examWeightTotal) : null;

            // Project score: weighted average
            double projWeightedSum = 0, projWeightTotal = 0;
            List<ProjectScore> studentScores = scoresByStudent.getOrDefault(sid, List.of());
            for (ProjectScore ps : studentScores) {
                Project p = projects.stream().filter(pr -> pr.getId().equals(ps.getProjectId())).findFirst().orElse(null);
                if (p == null) continue;
                double w = p.getWeight() != null ? p.getWeight().doubleValue() : 1.0;
                projWeightedSum += GS.getOrDefault(ps.getGrade(), 0) * w;
                projWeightTotal += w;
            }
            Double projectScore = projWeightTotal > 0 ? round(projWeightedSum / projWeightTotal) : null;

            // Result score
            double resultSum = 0, resultTotal = 0;
            if (examScore != null) { resultSum += examScore; resultTotal++; }
            if (projectScore != null) { resultSum += projectScore; resultTotal++; }
            Double resultScore = resultTotal > 0 ? round(resultSum / resultTotal) : null;

            // Total score
            Double totalScore = (processScore != null && resultScore != null) ?
                    round(processScore * 0.5 + resultScore * 0.5) : null;

            // Grade
            String totalGrade = totalScore != null ? gradeLabel(totalScore) : "暂无数据";

            String remark = "";
            if (processScore == null && resultScore == null) remark = "无评价数据";
            else if (processScore == null) remark = "缺过程评价";
            else if (resultScore == null) remark = "缺结果评价";

            rows.add(new GradeRow(
                    sc != null ? sc.getGrade() + "级" + sc.getName() : "",
                    sc != null ? sc.getGrade() + "级" + sc.getName() : "",
                    u.getStudentNo(), u.getName(),
                    awareness, computing, digitalLearn, responsibility,
                    processScore, examScore, projectScore,
                    resultScore, totalScore, totalGrade, remark));
        }
        return rows;
    }

    public byte[] exportExcel(Long semesterId) {
        List<GradeRow> rows = calculateSemesterGrades(semesterId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, GradeRow.class)
                .sheet("学期总评")
                .doWrite(rows);
        auditLogService.record("导出总评Excel", "semester", semesterId, "导出" + rows.size() + "条");
        return out.toByteArray();
    }

    private static Integer avgOrNull(List<Integer> scores) {
        if (scores.isEmpty()) return null;
        return (int) Math.round(scores.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private static Double round(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static String gradeLabel(double s) {
        if (s >= 90) return "A"; if (s >= 75) return "B"; if (s >= 60) return "C";
        if (s >= 40) return "D"; return "E";
    }
}
