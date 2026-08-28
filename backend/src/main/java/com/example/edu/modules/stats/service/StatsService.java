package com.example.edu.modules.stats.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.course.entity.AssessmentScheme;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.Lesson;
import com.example.edu.modules.course.entity.Semester;
import com.example.edu.modules.course.mapper.AssessmentSchemeMapper;
import com.example.edu.modules.course.mapper.CourseMapper;
import com.example.edu.modules.course.mapper.LessonMapper;
import com.example.edu.modules.course.mapper.SemesterMapper;
import com.example.edu.modules.course.mapper.CourseClassMapper;
import com.example.edu.modules.course.entity.CourseClass;
import com.example.edu.modules.course.service.CoursePermissionHelper;
import com.example.edu.modules.evaluation.entity.DimensionScore;
import com.example.edu.modules.evaluation.mapper.DimensionScoreMapper;
import com.example.edu.modules.exam.entity.Exam;
import com.example.edu.modules.exam.entity.ExamSubmission;
import com.example.edu.modules.exam.mapper.ExamMapper;
import com.example.edu.modules.exam.mapper.ExamSubmissionMapper;
import com.example.edu.modules.project.entity.Project;
import com.example.edu.modules.project.entity.ProjectSubmission;
import com.example.edu.modules.project.mapper.ProjectMapper;
import com.example.edu.modules.project.mapper.ProjectSubmissionMapper;
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

    private static final List<String> DIMS = List.of("AWARENESS", "COMPUTING", "DIGITAL_LEARNING", "RESPONSIBILITY");

    private final DimensionScoreMapper dimensionScoreMapper;
    private final ExamSubmissionMapper examSubmissionMapper;
    private final ExamMapper examMapper;
    private final ProjectSubmissionMapper projectSubmissionMapper;
    private final ProjectMapper projectMapper;
    private final SubmissionMapper submissionMapper;
    private final TaskMapper taskMapper;
    private final LessonMapper lessonMapper;
    private final SemesterMapper semesterMapper;
    private final CourseMapper courseMapper;
    private final AssessmentSchemeMapper assessmentSchemeMapper;
    private final UserMapper userMapper;
    private final SchoolClassMapper schoolClassMapper;
    private final CourseClassMapper courseClassMapper;
    private final AuditLogService auditLogService;

    public record GradeRow(
            Long studentId,
            String school, String className, String studentNo, String studentName,
            Integer awareness, Integer computing, Integer digitalLearn, Integer responsibility,
            Double processScore, Double examScore, Double projectScore,
            Double resultScore, Double totalScore, String totalGrade, String remark) {}

    public List<GradeRow> calculateSemesterGrades(Long semesterId) {
        Course course = checkSemesterAccess(semesterId);
        AssessmentScheme scheme = getScheme(semesterId);
        List<User> courseStudents = getCourseStudents(course.getId());
        // 1. Collect all tasks in this semester's lessons
        List<Lesson> lessons = lessonMapper.selectList(
                new LambdaQueryWrapper<Lesson>().eq(Lesson::getSemesterId, semesterId));
        List<Long> lessonIds = lessons.stream().map(Lesson::getId).toList();
        List<Task> tasks = lessonIds.isEmpty() ? List.of() : taskMapper.selectList(
                new LambdaQueryWrapper<Task>().in(Task::getLessonId, lessonIds));

        // 2. Get all submissions in one batch query
        List<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toList());
        List<Submission> allSubs = taskIds.isEmpty() ? List.of() :
                submissionMapper.selectList(new LambdaQueryWrapper<Submission>().in(Submission::getTaskId, taskIds));
        Set<Long> studentIds = courseStudents.stream().map(User::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 3. Get numeric process dimension scores
        List<Long> submissionIds = allSubs.stream().map(Submission::getId).collect(Collectors.toList());
        List<DimensionScore> processScores = submissionIds.isEmpty() ? List.of() :
                dimensionScoreMapper.selectList(new LambdaQueryWrapper<DimensionScore>()
                        .eq(DimensionScore::getSourceType, "process")
                        .in(DimensionScore::getSourceId, submissionIds));

        // 4. Get exams (batch)
        List<Exam> exams = examMapper.selectList(
                new LambdaQueryWrapper<Exam>().eq(Exam::getSemesterId, semesterId));
        List<Long> examIds = exams.stream().map(Exam::getId).collect(Collectors.toList());
        List<Long> examSubmissionIds = new ArrayList<>();
        if (!examIds.isEmpty()) {
            List<ExamSubmission> allExamSubs = examSubmissionMapper.selectList(
                    new LambdaQueryWrapper<ExamSubmission>().in(ExamSubmission::getExamId, examIds));
            examSubmissionIds = allExamSubs.stream().map(ExamSubmission::getId).toList();
        }
        List<DimensionScore> examDimensionScores = examSubmissionIds.isEmpty() ? List.of() :
                dimensionScoreMapper.selectList(new LambdaQueryWrapper<DimensionScore>()
                        .eq(DimensionScore::getSourceType, "exam")
                        .in(DimensionScore::getSourceId, examSubmissionIds));

        // 5. Get projects (batch)
        List<Project> projects = projectMapper.selectList(
                new LambdaQueryWrapper<Project>().eq(Project::getSemesterId, semesterId));
        List<Long> projectIds = projects.stream().map(Project::getId).collect(Collectors.toList());
        List<ProjectSubmission> projectSubs = projectIds.isEmpty() ? List.of() :
                projectSubmissionMapper.selectList(new LambdaQueryWrapper<ProjectSubmission>()
                        .in(ProjectSubmission::getProjectId, projectIds));
        List<Long> projectSubmissionIds = projectSubs.stream().map(ProjectSubmission::getId).toList();
        List<DimensionScore> projectDimensionScores = List.of();
        if (!projectSubmissionIds.isEmpty()) {
            projectDimensionScores = dimensionScoreMapper.selectList(new LambdaQueryWrapper<DimensionScore>()
                    .eq(DimensionScore::getSourceType, "project")
                    .in(DimensionScore::getSourceId, projectSubmissionIds));
        }

        if (studentIds.isEmpty()) return List.of();
        Map<Long, User> userMap = userMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Set<Long> classIds = userMap.values().stream()
                .map(User::getClassId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SchoolClass> classMap = classIds.isEmpty() ? Map.of() :
                schoolClassMapper.selectBatchIds(classIds).stream()
                        .collect(Collectors.toMap(SchoolClass::getId, c -> c));

        Map<String, Map<Long, Map<String, ScoreBucket>>> buckets = new HashMap<>();
        buckets.put("process", bucketByStudentAndDimension(processScores));
        buckets.put("exam", bucketByStudentAndDimension(examDimensionScores));
        buckets.put("project", bucketByStudentAndDimension(projectDimensionScores));

        // 6. Calculate per student
        List<GradeRow> rows = new ArrayList<>();
        for (Long sid : studentIds) {
            User u = userMap.get(sid);
            if (u == null) continue;
            SchoolClass sc = u.getClassId() != null ? classMap.get(u.getClassId()) : null;

            Map<String, Double> processDim = dimensionRates(buckets.get("process").get(sid));
            Map<String, Double> examDim = dimensionRates(buckets.get("exam").get(sid));
            Map<String, Double> projectDim = dimensionRates(buckets.get("project").get(sid));

            Double processScore = averageDimensionScore(processDim);
            Double examScore = averageDimensionScore(examDim);
            Double projectScore = averageDimensionScore(projectDim);

            Map<String, Double> finalDim = new LinkedHashMap<>();
            for (String d : DIMS) {
                finalDim.put(d, calculateWeightedScore(
                        Arrays.asList(processDim.get(d), examDim.get(d), projectDim.get(d)),
                        Arrays.asList(scheme.getProcessPercent(), scheme.getExamPercent(), scheme.getProjectPercent())));
            }

            Integer awareness = intOrNull(finalDim.get("AWARENESS"));
            Integer computing = intOrNull(finalDim.get("COMPUTING"));
            Integer digitalLearn = intOrNull(finalDim.get("DIGITAL_LEARNING"));
            Integer responsibility = intOrNull(finalDim.get("RESPONSIBILITY"));

            Double resultScore = calculateWeightedScore(
                    Arrays.asList(examScore, projectScore),
                    Arrays.asList(scheme.getExamPercent(), scheme.getProjectPercent()));
            Double totalScore = averageDimensionScore(finalDim);

            // Grade
            String totalGrade = totalScore != null ? gradeLabel(totalScore) : "暂无数据";

            String remark = "";
            if (totalScore == null) remark = "无可折算评价数据";
            else if (scheme.getProcessPercent() > 0 && processScore == null) remark = "缺平时任务成绩";
            else if (scheme.getExamPercent() > 0 && examScore == null) remark = "缺考试成绩";
            else if (scheme.getProjectPercent() > 0 && projectScore == null) remark = "缺项目成绩";

            String className = sc != null ? sc.getGrade() + "级" + sc.getName() : "";
            rows.add(new GradeRow(
                    sid,
                    "", // school field reserved for future use
                    className,
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

    private static Double round(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static Map<Long, Map<String, ScoreBucket>> bucketByStudentAndDimension(List<DimensionScore> scores) {
        Map<Long, Map<String, ScoreBucket>> result = new HashMap<>();
        for (DimensionScore score : scores) {
            if (score.getMaxScore() == null || score.getMaxScore().compareTo(BigDecimal.ZERO) <= 0) continue;
            result.computeIfAbsent(score.getStudentId(), ignored -> new HashMap<>())
                    .computeIfAbsent(score.getDimension(), ignored -> new ScoreBucket())
                    .add(score.getEarnedScore(), score.getMaxScore());
        }
        return result;
    }

    private static Map<String, Double> dimensionRates(Map<String, ScoreBucket> buckets) {
        if (buckets == null || buckets.isEmpty()) return Map.of();
        Map<String, Double> result = new HashMap<>();
        for (String dim : DIMS) {
            ScoreBucket bucket = buckets.get(dim);
            if (bucket != null && bucket.max.compareTo(BigDecimal.ZERO) > 0) {
                result.put(dim, round(bucket.earned.divide(bucket.max, 6, RoundingMode.HALF_UP).doubleValue() * 100));
            }
        }
        return result;
    }

    private static Double averageDimensionScore(Map<String, Double> scores) {
        if (scores == null || scores.isEmpty()) return null;
        double sum = 0;
        int count = 0;
        for (String dim : DIMS) {
            Double score = scores.get(dim);
            if (score == null) continue;
            sum += score;
            count++;
        }
        return count > 0 ? round(sum / count) : null;
    }

    private static Integer intOrNull(Double score) {
        return score == null ? null : (int) Math.round(score);
    }

    private AssessmentScheme getScheme(Long semesterId) {
        AssessmentScheme scheme = assessmentSchemeMapper == null ? null : assessmentSchemeMapper.selectOne(new LambdaQueryWrapper<AssessmentScheme>()
                .eq(AssessmentScheme::getSemesterId, semesterId));
        if (scheme != null) return scheme;
        scheme = new AssessmentScheme();
        scheme.setSemesterId(semesterId);
        scheme.setProcessPercent(50);
        scheme.setExamPercent(50);
        scheme.setProjectPercent(0);
        return scheme;
    }

    private Course checkSemesterAccess(Long semesterId) {
        Semester semester = semesterMapper.selectById(semesterId);
        if (semester == null) {
            throw new BizException(ErrorCode.SEMESTER_NOT_FOUND);
        }
        Course course = courseMapper.selectById(semester.getCourseId());
        if (course == null) {
            throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        }
        CoursePermissionHelper.checkTeacherOwnsCourse(course);
        return course;
    }

    private List<User> getCourseStudents(Long courseId) {
        List<CourseClass> bindings = Optional.ofNullable(courseClassMapper)
                .map(mapper -> mapper.selectList(new LambdaQueryWrapper<CourseClass>()
                        .eq(CourseClass::getCourseId, courseId)))
                .orElse(List.of());
        Set<Long> classIds = bindings.stream().map(CourseClass::getClassId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (classIds.isEmpty()) return List.of();
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "student")
                .in(User::getClassId, classIds));
    }

    private static Double calculateWeightedScore(List<Double> scores, List<Integer> percents) {
        double sum = 0;
        double weight = 0;
        for (int i = 0; i < scores.size(); i++) {
            Integer percent = percents.get(i);
            if (percent == null || percent <= 0) continue;
            Double score = scores.get(i);
            if (score == null) continue;
            sum += score * percent;
            weight += percent;
        }
        return weight > 0 ? round(sum / weight) : null;
    }

    private static String gradeLabel(double s) {
        if (s >= 90) return "A"; if (s >= 75) return "B"; if (s >= 60) return "C";
        if (s >= 40) return "D"; return "E";
    }

    private static class ScoreBucket {
        private BigDecimal earned = BigDecimal.ZERO;
        private BigDecimal max = BigDecimal.ZERO;

        private void add(BigDecimal earnedScore, BigDecimal maxScore) {
            earned = earned.add(earnedScore == null ? BigDecimal.ZERO : earnedScore);
            max = max.add(maxScore == null ? BigDecimal.ZERO : maxScore);
        }
    }
}
