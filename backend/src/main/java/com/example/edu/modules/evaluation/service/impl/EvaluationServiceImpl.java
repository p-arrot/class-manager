package com.example.edu.modules.evaluation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.Lesson;
import com.example.edu.modules.course.entity.Semester;
import com.example.edu.modules.course.mapper.*;
import com.example.edu.modules.course.service.CoursePermissionHelper;
import com.example.edu.modules.evaluation.dto.EvaluateDTO;
import com.example.edu.modules.evaluation.entity.Evaluation;
import com.example.edu.modules.evaluation.enums.Grade;
import com.example.edu.modules.evaluation.mapper.EvaluationMapper;
import com.example.edu.modules.evaluation.service.DimensionScoreService;
import com.example.edu.modules.evaluation.service.EvaluationService;
import com.example.edu.modules.evaluation.vo.EvaluationVO;
import com.example.edu.modules.evaluation.vo.RadarVO;
import com.example.edu.modules.task.entity.Submission;
import com.example.edu.modules.task.entity.Task;
import com.example.edu.modules.task.mapper.SubmissionMapper;
import com.example.edu.modules.task.mapper.TaskMapper;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private static final Map<String, Integer> GRADE_SCORES = Arrays.stream(Grade.values())
            .collect(Collectors.toUnmodifiableMap(Grade::name, Grade::getScore));
    private static final Map<String, String> DIMENSION_LABELS = Map.of(
            "AWARENESS", "信息意识",
            "COMPUTING", "计算思维",
            "DIGITAL_LEARNING", "数字化学习与创新",
            "RESPONSIBILITY", "信息社会责任"
    );

    private final EvaluationMapper evaluationMapper;
    private final SubmissionMapper submissionMapper;
    private final TaskMapper taskMapper;
    private final LessonMapper lessonMapper;
    private final SemesterMapper semesterMapper;
    private final CourseMapper courseMapper;
    private final CourseClassMapper courseClassMapper;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;
    private final DimensionScoreService dimensionScoreService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void evaluate(Long submissionId, EvaluateDTO dto) {
        Submission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BizException(ErrorCode.SUBMISSION_NOT_FOUND);
        Task task = taskMapper.selectById(sub.getTaskId());
        if (task == null) throw new BizException(ErrorCode.TASK_NOT_FOUND);

        checkTaskOwner(task);

        boolean isSpecial = dto.getIsSpecial() != null && dto.getIsSpecial();
        boolean hasDimensions = dto.getDimensions() != null && !dto.getDimensions().isEmpty();
        boolean hasQuestionScores = dto.getQuestionScores() != null && !dto.getQuestionScores().isEmpty();

        if (isSpecial) sub.setStatus("special");
        else if (hasDimensions || hasQuestionScores) sub.setStatus("graded");
        else sub.setStatus("submitted"); // unmark: reset to submitted
        submissionMapper.updateById(sub);

        // Delete old evaluations for this submission
        evaluationMapper.delete(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getSourceType, task.getType())
                .eq(Evaluation::getSourceId, submissionId));

        if (hasDimensions) {
            for (EvaluateDTO.DimensionGrade dg : dto.getDimensions()) {
                Evaluation eval = new Evaluation();
                eval.setStudentId(sub.getStudentId());
                eval.setSourceType(task.getType());
                eval.setSourceId(submissionId);
                eval.setDimension(dg.getDimension());
                eval.setGrade(dg.getGrade());
                eval.setIsSpecial(0);
                evaluationMapper.insert(eval);
            }
        }
        if (isSpecial) {
            Evaluation eval = new Evaluation();
            eval.setStudentId(sub.getStudentId());
            eval.setSourceType(task.getType());
            eval.setSourceId(submissionId);
            eval.setDimension("AWARENESS");
            eval.setGrade("F");
            eval.setIsSpecial(1);
            evaluationMapper.insert(eval);
        }

        if (isSpecial || hasQuestionScores) {
            List<DimensionScoreService.ScoreInput> scoreInputs = hasQuestionScores
                    ? dto.getQuestionScores().stream()
                            .map(score -> new DimensionScoreService.ScoreInput(
                                    score.getQuestionId(),
                                    score.getDimension(),
                                    score.getEarnedScore(),
                                    score.getMaxScore(),
                                    Boolean.TRUE.equals(score.getAutoGraded())
                            ))
                            .toList()
                    : List.of();
            dimensionScoreService.replaceScores("process", submissionId, sub.getStudentId(), scoreInputs);
        }

        auditLogService.record("评分", "submission", submissionId,
                "标记=" + (isSpecial ? "特殊情况" : "已评分"));
    }

    @Override
    public List<EvaluationVO> getStudentEvaluations(Long studentId, Long semesterId) {
        // Get all tasks in this semester
        List<Task> tasks = getTasksInSemester(semesterId);
        Set<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toSet());
        if (taskIds.isEmpty()) return List.of();

        List<Evaluation> evals = evaluationMapper.selectList(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getStudentId, studentId)
                        .eq(Evaluation::getIsSpecial, 0)
                        .in(Evaluation::getSourceType, List.of("worksheet", "artifact")));

        return evals.stream()
                .map(e -> EvaluationVO.builder()
                        .dimension(e.getDimension())
                        .grade(e.getGrade())
                        .score(GRADE_SCORES.getOrDefault(e.getGrade(), 0))
                        .label(DIMENSION_LABELS.getOrDefault(e.getDimension(), e.getDimension()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public RadarVO getRadar(Long studentId, Long semesterId) {
        List<EvaluationVO> current = getStudentEvaluations(studentId, semesterId);
        Map<String, Double> currentAvgs = avgByDimension(current);

        List<RadarVO.DimensionScore> currentScores = DIMENSION_LABELS.entrySet().stream()
                .map(e -> RadarVO.DimensionScore.builder()
                        .dimension(e.getKey())
                        .label(e.getValue())
                        .avgScore(currentAvgs.getOrDefault(e.getKey(), 0.0))
                        .build())
                .collect(Collectors.toList());

        // Previous semester radar
        Semester semester = semesterMapper.selectById(semesterId);
        List<RadarVO.DimensionScore> previousScores = List.of();
        boolean hasPrevious = false;
        if (semester != null) {
            Semester prev = findPreviousSemester(semester);
            if (prev != null) {
                List<EvaluationVO> prevEvals = getStudentEvaluations(studentId, prev.getId());
                if (!prevEvals.isEmpty()) {
                    hasPrevious = true;
                    Map<String, Double> prevAvgs = avgByDimension(prevEvals);
                    previousScores = DIMENSION_LABELS.entrySet().stream()
                            .map(e -> RadarVO.DimensionScore.builder()
                                    .dimension(e.getKey())
                                    .label(e.getValue())
                                    .avgScore(prevAvgs.getOrDefault(e.getKey(), 0.0))
                                    .build())
                            .collect(Collectors.toList());
                }
            }
        }

        return RadarVO.builder()
                .current(currentScores)
                .previous(previousScores)
                .hasPrevious(hasPrevious)
                .build();
    }

    @Override
    public Map<String, Integer> getGradeScores() {
        return GRADE_SCORES;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoGradeMissedDeadlines(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || task.getDeadline() == null) return;

        List<Submission> subs = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>().eq(Submission::getTaskId, taskId));

        // Get all students in the course's classes
        Lesson lesson = lessonMapper.selectById(task.getLessonId());
        Semester semester = semesterMapper.selectById(lesson.getSemesterId());
        Course course = courseMapper.selectById(semester.getCourseId());
        List<com.example.edu.modules.course.entity.CourseClass> bindings =
                courseClassMapper.selectList(new LambdaQueryWrapper<com.example.edu.modules.course.entity.CourseClass>()
                        .eq(com.example.edu.modules.course.entity.CourseClass::getCourseId, course.getId()));
        Set<Long> classIds = bindings.stream().map(com.example.edu.modules.course.entity.CourseClass::getClassId).collect(Collectors.toSet());

        List<User> students = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, "student")
                        .in(User::getClassId, classIds));
        Set<Long> submittedIds = subs.stream().map(Submission::getStudentId).collect(Collectors.toSet());

        for (User student : students) {
            if (!submittedIds.contains(student.getId())) {
                Submission sub = new Submission();
                sub.setTaskId(taskId);
                sub.setStudentId(student.getId());
                sub.setStatus("graded");
                sub.setContent("{\"auto\":\"F\"}");
                sub.setSubmittedAt(java.time.LocalDateTime.now());
                submissionMapper.insert(sub);

                for (String dim : DIMENSION_LABELS.keySet()) {
                    Evaluation eval = new Evaluation();
                    eval.setStudentId(student.getId());
                    eval.setSourceType(task.getType());
                    eval.setSourceId(sub.getId());
                    eval.setDimension(dim);
                    eval.setGrade("F");
                    eval.setIsSpecial(0);
                    evaluationMapper.insert(eval);
                }
            }
        }
    }

    // ========== helpers ==========

    private Map<String, Double> avgByDimension(List<EvaluationVO> evals) {
        return evals.stream()
                .collect(Collectors.groupingBy(EvaluationVO::getDimension,
                        Collectors.averagingDouble(EvaluationVO::getScore)));
    }

    private List<Task> getTasksInSemester(Long semesterId) {
        List<Lesson> lessons = lessonMapper.selectList(
                new LambdaQueryWrapper<Lesson>().eq(Lesson::getSemesterId, semesterId));
        if (lessons.isEmpty()) return List.of();
        return taskMapper.selectList(
                new LambdaQueryWrapper<Task>().in(Task::getLessonId,
                        lessons.stream().map(Lesson::getId).collect(Collectors.toList())));
    }

    private Semester findPreviousSemester(Semester current) {
        return semesterMapper.selectList(
                        new LambdaQueryWrapper<Semester>()
                                .eq(Semester::getCourseId, current.getCourseId())
                                .lt(Semester::getStartTime, current.getStartTime())
                                .orderByDesc(Semester::getStartTime)
                                .last("LIMIT 1"))
                .stream().findFirst().orElse(null);
    }

    private void checkTaskOwner(Task task) {
        Lesson lesson = lessonMapper.selectById(task.getLessonId());
        if (lesson == null) throw new BizException(ErrorCode.LESSON_NOT_FOUND);
        Semester semester = semesterMapper.selectById(lesson.getSemesterId());
        if (semester == null) throw new BizException(ErrorCode.SEMESTER_NOT_FOUND);
        Course course = courseMapper.selectById(semester.getCourseId());
        if (course == null) throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        CoursePermissionHelper.checkTeacherOwnsCourse(course);
    }
}
