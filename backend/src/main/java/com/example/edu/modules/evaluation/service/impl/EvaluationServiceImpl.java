package com.example.edu.modules.evaluation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.common.submission.SubmissionStatus;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.Lesson;
import com.example.edu.modules.course.entity.Semester;
import com.example.edu.modules.course.mapper.*;
import com.example.edu.modules.course.service.CoursePermissionHelper;
import com.example.edu.modules.evaluation.dto.EvaluateDTO;
import com.example.edu.modules.evaluation.entity.Evaluation;
import com.example.edu.modules.evaluation.entity.SubmissionFeedback;
import com.example.edu.modules.evaluation.enums.Grade;
import com.example.edu.modules.evaluation.mapper.EvaluationMapper;
import com.example.edu.modules.evaluation.mapper.SubmissionFeedbackMapper;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private static final ObjectMapper JSON = new ObjectMapper();
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
    private final SubmissionFeedbackMapper submissionFeedbackMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void evaluate(Long submissionId, EvaluateDTO dto) {
        Submission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BizException(ErrorCode.SUBMISSION_NOT_FOUND);
        Task task = taskMapper.selectById(sub.getTaskId());
        if (task == null) throw new BizException(ErrorCode.TASK_NOT_FOUND);

        checkTaskOwner(task);
        if (SubmissionStatus.RETURNED.equals(sub.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "学生尚未重新提交，不能批改");
        }

        boolean isSpecial = dto.getIsSpecial() != null && dto.getIsSpecial();
        boolean hasDimensions = dto.getDimensions() != null && !dto.getDimensions().isEmpty();
        boolean hasQuestionScores = dto.getQuestionScores() != null && !dto.getQuestionScores().isEmpty();
        boolean hasFeedback = hasText(dto.getTeacherComment())
                || (dto.getQuestionFeedback() != null && !dto.getQuestionFeedback().isEmpty());

        if (isSpecial) sub.setStatus("special");
        else if (hasDimensions || hasQuestionScores || hasFeedback) sub.setStatus("graded");
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

        if (isSpecial || hasDimensions || hasQuestionScores || hasFeedback) {
            saveFeedback(submissionId, dto);
        } else {
            submissionFeedbackMapper.deleteById(submissionId);
        }

        auditLogService.record("评分", "submission", submissionId,
                "标记=" + (isSpecial ? "特殊情况" : "已评分"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnSubmission(Long submissionId, String reason) {
        Submission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BizException(ErrorCode.SUBMISSION_NOT_FOUND);
        Task task = taskMapper.selectById(sub.getTaskId());
        if (task == null) throw new BizException(ErrorCode.TASK_NOT_FOUND);
        checkTaskOwner(task);

        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isEmpty()) throw new BizException(ErrorCode.BAD_REQUEST, "请填写退回原因");

        sub.setStatus(SubmissionStatus.RETURNED);
        sub.setReturnReason(normalizedReason);
        sub.setReturnedAt(LocalDateTime.now());
        submissionMapper.updateById(sub);
        evaluationMapper.delete(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getSourceType, task.getType())
                .eq(Evaluation::getSourceId, submissionId));
        dimensionScoreService.clearScores("process", submissionId);
        submissionFeedbackMapper.deleteById(submissionId);
        auditLogService.record("退回任务修改", "submission", submissionId, normalizedReason);
    }

    @Override
    public List<EvaluationVO> getStudentEvaluations(Long studentId, Long semesterId) {
        checkStudentViewAccess(studentId, semesterId);
        // Get all tasks in this semester
        List<Task> tasks = getTasksInSemester(semesterId);
        Set<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toSet());
        if (taskIds.isEmpty()) return List.of();
        Map<Long, Task> taskById = tasks.stream()
                .collect(Collectors.toMap(Task::getId, task -> task));

        List<Submission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getStudentId, studentId)
                        .in(Submission::getTaskId, taskIds));
        if (submissions.isEmpty()) return List.of();

        Map<Long, Submission> submissionById = submissions.stream()
                .collect(Collectors.toMap(Submission::getId, submission -> submission));
        Set<Long> submissionIds = submissionById.keySet();

        List<Evaluation> evals = evaluationMapper.selectList(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getStudentId, studentId)
                        .eq(Evaluation::getIsSpecial, 0)
                        .in(Evaluation::getSourceType, List.of("worksheet", "artifact"))
                        .in(Evaluation::getSourceId, submissionIds));

        return evals.stream()
                .map(e -> {
                    Submission submission = submissionById.get(e.getSourceId());
                    Task task = submission != null ? taskById.get(submission.getTaskId()) : null;
                    return EvaluationVO.builder()
                        .sourceType(e.getSourceType())
                        .sourceId(e.getSourceId())
                        .submissionId(submission != null ? submission.getId() : null)
                        .taskId(submission != null ? submission.getTaskId() : null)
                        .taskTitle(task != null ? task.getTitle() : null)
                        .taskStatus(submission != null ? submission.getStatus() : null)
                        .dimension(e.getDimension())
                        .grade(e.getGrade())
                        .score(GRADE_SCORES.getOrDefault(e.getGrade(), 0))
                        .label(DIMENSION_LABELS.getOrDefault(e.getDimension(), e.getDimension()))
                        .evaluatedAt(e.getCreatedAt())
                        .build();
                })
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
        checkTaskOwner(task);

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

    private void checkStudentViewAccess(Long studentId, Long semesterId) {
        String role = SecurityUtils.getCurrentUserRole();
        if ("student".equals(role)
                && !Objects.equals(studentId, SecurityUtils.getCurrentUserId())) {
            throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
        }
        Semester semester = semesterMapper.selectById(semesterId);
        if (semester == null) throw new BizException(ErrorCode.SEMESTER_NOT_FOUND);
        Course course = courseMapper.selectById(semester.getCourseId());
        if (course == null) throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        if ("student".equals(role)) {
            CoursePermissionHelper.checkCourseAccess(course, courseClassMapper);
        } else if ("teacher".equals(role)) {
            CoursePermissionHelper.checkTeacherOwnsCourse(course);
            ensureStudentInCourse(studentId, course.getId());
        } else if (!"admin".equals(role)) {
            throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
        }
    }

    private void ensureStudentInCourse(Long studentId, Long courseId) {
        User student = userMapper.selectById(studentId);
        if (student == null || student.getClassId() == null) {
            throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
        }
        Long count = courseClassMapper.selectCount(new LambdaQueryWrapper<com.example.edu.modules.course.entity.CourseClass>()
                .eq(com.example.edu.modules.course.entity.CourseClass::getCourseId, courseId)
                .eq(com.example.edu.modules.course.entity.CourseClass::getClassId, student.getClassId()));
        if (count == null || count == 0) throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
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

    private void saveFeedback(Long submissionId, EvaluateDTO dto) {
        SubmissionFeedback feedback = new SubmissionFeedback();
        feedback.setSubmissionId(submissionId);
        feedback.setTeacherId(SecurityUtils.getCurrentUserId());
        feedback.setTeacherComment(dto.getTeacherComment());
        feedback.setQuestionFeedback(serializeQuestionFeedback(dto.getQuestionFeedback()));
        feedback.setGradedAt(LocalDateTime.now());
        if (submissionFeedbackMapper.selectById(submissionId) == null) {
            submissionFeedbackMapper.insert(feedback);
        } else {
            submissionFeedbackMapper.updateById(feedback);
        }
    }

    private String serializeQuestionFeedback(List<EvaluateDTO.QuestionFeedback> feedback) {
        if (feedback == null || feedback.isEmpty()) return null;
        try {
            return JSON.writeValueAsString(feedback);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "逐题反馈格式不正确");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
