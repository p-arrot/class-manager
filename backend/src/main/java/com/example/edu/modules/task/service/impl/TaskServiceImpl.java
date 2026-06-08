package com.example.edu.modules.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.CourseClass;
import com.example.edu.modules.course.entity.Lesson;
import com.example.edu.modules.course.entity.Semester;
import com.example.edu.modules.course.mapper.LessonMapper;
import com.example.edu.modules.course.mapper.SemesterMapper;
import com.example.edu.modules.course.mapper.CourseMapper;
import com.example.edu.modules.course.service.CoursePermissionHelper;
import com.example.edu.modules.classes.entity.TeacherClass;
import com.example.edu.modules.classes.mapper.TeacherClassMapper;
import com.example.edu.modules.course.mapper.CourseClassMapper;
import com.example.edu.modules.evaluation.service.DimensionScoreService;
import com.example.edu.modules.evaluation.service.QuestionScoreHelper;
import com.example.edu.modules.task.dto.SubmissionDTO;
import com.example.edu.modules.task.dto.TaskCreateDTO;
import com.example.edu.modules.task.dto.TaskUpdateDTO;
import com.example.edu.modules.task.entity.Submission;
import com.example.edu.modules.task.entity.Task;
import com.example.edu.modules.task.mapper.SubmissionMapper;
import com.example.edu.modules.task.mapper.TaskMapper;
import com.example.edu.modules.task.service.TaskService;
import com.example.edu.modules.realtime.service.RealtimeService;
import com.example.edu.modules.task.vo.SubmissionVO;
import com.example.edu.modules.task.vo.TaskAnalyticsVO;
import com.example.edu.modules.task.vo.TaskDetailVO;
import com.example.edu.modules.task.vo.TaskVO;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeParseException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final TaskMapper taskMapper;
    private final SubmissionMapper submissionMapper;
    private final LessonMapper lessonMapper;
    private final SemesterMapper semesterMapper;
    private final CourseMapper courseMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final CourseClassMapper courseClassMapper;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;
    private final RealtimeService realtimeService;
    private final DimensionScoreService dimensionScoreService;
    private final QuestionScoreHelper questionScoreHelper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskVO create(Long lessonId, TaskCreateDTO dto) {
        Lesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null) throw new BizException(ErrorCode.LESSON_NOT_FOUND);
        checkLessonOwner(lesson);

        Task task = new Task();
        task.setLessonId(lessonId);
        task.setTitle(dto.getTitle());
        task.setType(dto.getType());
        task.setFormSchema(dto.getFormSchema());
        task.setDescription(dto.getDescription());
        if (dto.getDeadline() != null) {
            task.setDeadline(parseDeadline(dto.getDeadline()));
        }
        taskMapper.insert(task);

        auditLogService.record("创建任务", "task", task.getId(), task.getTitle());
        return toVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskVO update(Long id, TaskUpdateDTO dto) {
        Task task = taskMapper.selectById(id);
        if (task == null) throw new BizException(ErrorCode.TASK_NOT_FOUND);
        checkTaskOwner(task);

        if (dto.getTitle() != null) task.setTitle(dto.getTitle());
        if (dto.getFormSchema() != null) task.setFormSchema(dto.getFormSchema());
        if (dto.getDescription() != null) task.setDescription(dto.getDescription());
        if (dto.getDeadline() != null) task.setDeadline(parseDeadline(dto.getDeadline()));
        taskMapper.updateById(task);

        auditLogService.record("更新任务", "task", id, task.getTitle());
        return toVO(taskMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) throw new BizException(ErrorCode.TASK_NOT_FOUND);
        checkTaskOwner(task);

        // Delete submissions first
        submissionMapper.delete(new LambdaQueryWrapper<Submission>().eq(Submission::getTaskId, id));
        taskMapper.deleteById(id);

        auditLogService.record("删除任务", "task", id, task.getTitle());
    }

    @Override
    public TaskDetailVO getById(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) throw new BizException(ErrorCode.TASK_NOT_FOUND);
        checkTaskAccess(task);

        int count = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>().eq(Submission::getTaskId, id)).intValue();

        return TaskDetailVO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .type(task.getType())
                .lessonId(task.getLessonId())
                .formSchema(task.getFormSchema())
                .description(task.getDescription())
                .deadline(task.getDeadline())
                .submissionCount(count)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    @Override
    public List<TaskVO> listByLessonId(Long lessonId) {
        Lesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null) throw new BizException(ErrorCode.LESSON_NOT_FOUND);
        checkLessonAccess(lesson);

        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getLessonId, lessonId)
                        .orderByDesc(Task::getCreatedAt));

        Set<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toSet());
        Map<Long, Integer> countMap = Map.of();
        if (!taskIds.isEmpty()) {
            List<Submission> subs = submissionMapper.selectList(
                    new LambdaQueryWrapper<Submission>().in(Submission::getTaskId, taskIds));
            countMap = subs.stream()
                    .collect(Collectors.groupingBy(Submission::getTaskId,
                            Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
        }
        Map<Long, Integer> finalCountMap = countMap;
        return tasks.stream().map(t -> toVO(t, finalCountMap.getOrDefault(t.getId(), 0))).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubmissionVO submit(Long taskId, SubmissionDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUserRole();
        if (!"student".equals(role)) throw new BizException(ErrorCode.TASK_SUBMIT_STUDENT_ONLY);

        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BizException(ErrorCode.TASK_NOT_FOUND);

        // Check deadline
        if (task.getDeadline() != null && LocalDateTime.now().isAfter(task.getDeadline())) {
            throw new BizException(ErrorCode.TASK_DEADLINE_PASSED);
        }

        // Check access (student must be in the course's class)
        checkTaskAccess(task);

        // Upsert submission
        Submission sub = submissionMapper.selectOne(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getTaskId, taskId)
                        .eq(Submission::getStudentId, userId));
        if (sub == null) {
            sub = new Submission();
            sub.setTaskId(taskId);
            sub.setStudentId(userId);
        }
        // Don't allow re-submit if already graded
        if ("graded".equals(sub.getStatus())) {
            throw new BizException(ErrorCode.SUBMISSION_ALREADY_GRADED);
        }
        sub.setContent(dto.getContent());
        sub.setStatus("submitted");
        sub.setSubmittedAt(LocalDateTime.now());
        if (sub.getId() == null) {
            submissionMapper.insert(sub);
        } else {
            submissionMapper.updateById(sub);
        }

        autoGradeWorksheet(task, sub);

        SubmissionVO vo = toSubmissionVO(sub);
        // Push real-time update to teachers subscribed to this task
        realtimeService.pushSubmissionUpdate(taskId, vo);
        return vo;
    }

    @Override
    public List<SubmissionVO> listSubmissions(Long taskId, Long classId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BizException(ErrorCode.TASK_NOT_FOUND);
        checkTaskOwner(task);

        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<Submission>()
                .eq(Submission::getTaskId, taskId);

        if (classId != null) {
            List<User> students = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getRole, "student")
                            .eq(User::getClassId, classId));
            if (students.isEmpty()) return List.of();
            wrapper.in(Submission::getStudentId,
                    students.stream().map(User::getId).collect(Collectors.toList()));
        }

        List<Submission> subs = submissionMapper.selectList(wrapper);
        Set<Long> studentIds = subs.stream().map(Submission::getStudentId).collect(Collectors.toSet());
        Map<Long, User> userMap = studentIds.isEmpty() ? Map.of() :
                userMapper.selectBatchIds(studentIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        return subs.stream().map(s -> toSubmissionVO(s, userMap.get(s.getStudentId()))).toList();
    }

    @Override
    public SubmissionVO getSubmission(Long id) {
        Submission sub = submissionMapper.selectById(id);
        if (sub == null) throw new BizException(ErrorCode.SUBMISSION_NOT_FOUND);
        Task task = taskMapper.selectById(sub.getTaskId());
        if (task == null) throw new BizException(ErrorCode.TASK_NOT_FOUND);
        checkTaskAccess(task);
        return toSubmissionVO(sub);
    }

    @Override
    public List<SubmissionVO> getStudentSubmissions(Long studentId, Long semesterId) {
        // Verify self or teacher access
        String role = SecurityUtils.getCurrentUserRole();
        Long userId = SecurityUtils.getCurrentUserId();
        if ("student".equals(role) && !userId.equals(studentId)) {
            throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
        }

        // Get all tasks in semester's lessons
        List<Lesson> lessons = lessonMapper.selectList(
                new LambdaQueryWrapper<Lesson>().eq(Lesson::getSemesterId, semesterId));
        List<Long> lessonIds = lessons.stream().map(Lesson::getId).toList();
        if (lessonIds.isEmpty()) return List.of();

        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>().in(Task::getLessonId, lessonIds));
        List<Long> taskIds = tasks.stream().map(Task::getId).toList();
        if (taskIds.isEmpty()) return List.of();

        List<Submission> subs = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getStudentId, studentId)
                        .in(Submission::getTaskId, taskIds));
        return subs.stream().map(this::toSubmissionVO).toList();
    }

    @Override
    public java.util.Map<String, Object> getSubmissionStats(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BizException(ErrorCode.TASK_NOT_FOUND);
        checkTaskOwner(task);

        List<Submission> subs = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>().eq(Submission::getTaskId, taskId));
        long submitted = subs.stream().filter(s -> "submitted".equals(s.getStatus())).count();
        long graded = subs.stream().filter(s -> "graded".equals(s.getStatus())).count();
        long special = subs.stream().filter(s -> "special".equals(s.getStatus())).count();

        long total = getCourseStudents(task).size();

        return java.util.Map.of(
                "total", total,
                "submitted", submitted,
                "graded", graded,
                "special", special,
                "notSubmitted", total - submitted - graded - special
        );
    }

    @Override
    public TaskAnalyticsVO getTaskAnalytics(Long taskId, Long classId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BizException(ErrorCode.TASK_NOT_FOUND);
        checkTaskOwner(task);

        List<User> students = getCourseStudents(task, classId);
        Map<Long, User> studentMap = students.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<Submission> subs = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getTaskId, taskId)
                        .orderByDesc(Submission::getSubmittedAt));
        if (classId != null) {
            Set<Long> studentIds = studentMap.keySet();
            subs = subs.stream()
                    .filter(sub -> studentIds.contains(sub.getStudentId()))
                    .toList();
        }
        long submitted = subs.stream().filter(s -> "submitted".equals(s.getStatus())).count();
        long graded = subs.stream().filter(s -> "graded".equals(s.getStatus())).count();
        long special = subs.stream().filter(s -> "special".equals(s.getStatus())).count();
        int completed = Math.toIntExact(submitted + graded + special);
        int total = students.size();

        List<Map<String, Object>> questionDefs = "worksheet".equals(task.getType())
                ? parseQuestions(task.getFormSchema())
                : List.of();
        List<TaskAnalyticsVO.QuestionAnalyticsVO> questions = buildQuestionAnalytics(questionDefs, subs, studentMap);
        int autoQuestionCount = (int) questions.stream()
                .filter(TaskAnalyticsVO.QuestionAnalyticsVO::getAutoGradable)
                .count();
        double accuracyRate = questions.stream()
                .filter(TaskAnalyticsVO.QuestionAnalyticsVO::getAutoGradable)
                .mapToDouble(TaskAnalyticsVO.QuestionAnalyticsVO::getAccuracyRate)
                .average()
                .orElse(0);

        return TaskAnalyticsVO.builder()
                .taskId(task.getId())
                .title(task.getTitle())
                .type(task.getType())
                .totalStudents(total)
                .submittedCount(completed)
                .gradedCount(Math.toIntExact(graded))
                .specialCount(Math.toIntExact(special))
                .notSubmittedCount(Math.max(total - completed, 0))
                .submissionRate(percent(completed, total))
                .accuracyRate(round(accuracyRate))
                .questionCount(questions.size())
                .autoQuestionCount(autoQuestionCount)
                .manualQuestionCount(questions.size() - autoQuestionCount)
                .selectedClassId(classId)
                .classScopes(List.of())
                .questions(questions)
                .submissions(subs.stream().map(sub -> {
                    User student = studentMap.get(sub.getStudentId());
                    return TaskAnalyticsVO.StudentTaskAnswerVO.builder()
                            .submissionId(sub.getId())
                            .studentId(sub.getStudentId())
                            .studentName(student != null ? student.getName() : null)
                            .studentNo(student != null ? student.getStudentNo() : null)
                            .status(sub.getStatus())
                            .content(sub.getContent())
                            .submittedAt(sub.getSubmittedAt())
                            .build();
                }).toList())
                .build();
    }

    @Override
    public SubmissionVO getMySubmission(Long taskId, Long studentId) {
        Submission sub = submissionMapper.selectOne(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getTaskId, taskId)
                .eq(Submission::getStudentId, studentId));
        return sub != null ? toSubmissionVO(sub) : null;
    }

    // ========== private helpers ==========

    private void checkLessonOwner(Lesson lesson) {
        Semester semester = semesterMapper.selectById(lesson.getSemesterId());
        if (semester == null) throw new BizException(ErrorCode.SEMESTER_NOT_FOUND);
        Course course = courseMapper.selectById(semester.getCourseId());
        if (course == null) throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        CoursePermissionHelper.checkTeacherOwnsCourse(course);
    }

    private void checkTaskOwner(Task task) {
        Lesson lesson = lessonMapper.selectById(task.getLessonId());
        if (lesson == null) throw new BizException(ErrorCode.LESSON_NOT_FOUND);
        checkLessonOwner(lesson);
    }

    private void checkTaskAccess(Task task) {
        String role = SecurityUtils.getCurrentUserRole();
        if ("admin".equals(role)) return;

        Lesson lesson = lessonMapper.selectById(task.getLessonId());
        if (lesson == null) throw new BizException(ErrorCode.LESSON_NOT_FOUND);
        Semester semester = semesterMapper.selectById(lesson.getSemesterId());
        if (semester == null) throw new BizException(ErrorCode.SEMESTER_NOT_FOUND);
        Course course = courseMapper.selectById(semester.getCourseId());
        if (course == null) throw new BizException(ErrorCode.COURSE_NOT_FOUND);

        if ("teacher".equals(role)) {
            CoursePermissionHelper.checkTeacherOwnsCourse(course);
            return;
        }
        if ("student".equals(role)) {
            CoursePermissionHelper.checkCourseAccess(course, courseClassMapper);
            return;
        }
        throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
    }

    private void checkLessonAccess(Lesson lesson) {
        Semester semester = semesterMapper.selectById(lesson.getSemesterId());
        if (semester == null) throw new BizException(ErrorCode.SEMESTER_NOT_FOUND);
        Course course = courseMapper.selectById(semester.getCourseId());
        if (course == null) throw new BizException(ErrorCode.COURSE_NOT_FOUND);

        String role = SecurityUtils.getCurrentUserRole();
        if ("admin".equals(role)) return;
        if ("teacher".equals(role)) {
            CoursePermissionHelper.checkTeacherOwnsCourse(course);
            return;
        }
        if ("student".equals(role)) {
            CoursePermissionHelper.checkCourseAccess(course, courseClassMapper);
            return;
        }
        throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
    }

    private TaskVO toVO(Task task) {
        return toVO(task, 0);
    }

    private TaskVO toVO(Task task, int submissionCount) {
        return TaskVO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .type(task.getType())
                .lessonId(task.getLessonId())
                .description(task.getDescription())
                .deadline(task.getDeadline())
                .submissionCount(submissionCount)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private SubmissionVO toSubmissionVO(Submission sub) {
        User student = userMapper.selectById(sub.getStudentId());
        return toSubmissionVO(sub, student);
    }

    private SubmissionVO toSubmissionVO(Submission sub, User student) {
        return SubmissionVO.builder()
                .id(sub.getId())
                .taskId(sub.getTaskId())
                .studentId(sub.getStudentId())
                .studentName(student != null ? student.getName() : null)
                .studentNo(student != null ? student.getStudentNo() : null)
                .status(sub.getStatus())
                .content(sub.getContent())
                .submittedAt(sub.getSubmittedAt())
                .createdAt(sub.getCreatedAt())
                .build();
    }

    private LocalDateTime parseDeadline(String deadline) {
        try {
            return LocalDateTime.parse(deadline);
        } catch (DateTimeParseException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "截止时间格式不正确");
        }
    }

    private void autoGradeWorksheet(Task task, Submission sub) {
        if (!"worksheet".equals(task.getType()) || task.getFormSchema() == null || sub.getContent() == null) return;
        try {
            List<DimensionScoreService.ScoreInput> scores = questionScoreHelper.autoGrade(task.getFormSchema(), sub.getContent());
            if (scores.isEmpty()) return;
            dimensionScoreService.replaceAutoScores("process", sub.getId(), sub.getStudentId(), scores);
            sub.setStatus("graded");
            submissionMapper.updateById(sub);
            log.debug("Auto graded worksheet: taskId={}, submissionId={}, scoreRows={}", task.getId(), sub.getId(), scores.size());
        } catch (Exception e) {
            log.warn("Worksheet auto grade skipped: taskId={}, submissionId={}", task.getId(), sub.getId(), e);
        }
    }

    private List<User> getCourseStudents(Task task) {
        return getCourseStudents(task, null);
    }

    private List<User> getCourseStudents(Task task, Long classId) {
        Lesson lesson = lessonMapper.selectById(task.getLessonId());
        if (lesson == null) throw new BizException(ErrorCode.LESSON_NOT_FOUND);
        Semester semester = semesterMapper.selectById(lesson.getSemesterId());
        if (semester == null) throw new BizException(ErrorCode.SEMESTER_NOT_FOUND);
        Course course = courseMapper.selectById(semester.getCourseId());
        if (course == null) throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        List<CourseClass> bindings = courseClassMapper.selectList(
                new LambdaQueryWrapper<CourseClass>().eq(CourseClass::getCourseId, course.getId()));
        Set<Long> classIds = bindings.stream().map(CourseClass::getClassId).collect(Collectors.toSet());
        if (classIds.isEmpty()) return List.of();
        if (classId != null) {
            if (!classIds.contains(classId)) {
                throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
            }
            classIds = Set.of(classId);
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "student")
                .in(User::getClassId, classIds));
    }

    private List<Map<String, Object>> parseQuestions(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) return List.of();
        try {
            Map<String, Object> schema = JSON.readValue(schemaJson, new TypeReference<>() {});
            Object questions = schema.get("questions");
            if (!(questions instanceof List<?>)) {
                questions = schema.get("fields");
            }
            if (!(questions instanceof List<?> list)) return List.of();
            return list.stream()
                    .map(this::asStringObjectMap)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.debug("Task analytics ignored invalid schema", e);
            return List.of();
        }
    }

    private Map<String, Object> asStringObjectMap(Object value) {
        if (!(value instanceof Map<?, ?> raw)) return null;
        Map<String, Object> normalized = new LinkedHashMap<>();
        raw.forEach((key, item) -> normalized.put(String.valueOf(key), item));
        if (!normalized.containsKey("stem") && normalized.containsKey("label")) {
            normalized.put("stem", normalized.get("label"));
        }
        Object type = normalized.get("type");
        if ("radio".equals(type)) normalized.put("type", "single");
        if ("checkbox".equals(type)) normalized.put("type", "multiple");
        if ("textarea".equals(type)) normalized.put("type", "short");
        return normalized;
    }

    private List<TaskAnalyticsVO.QuestionAnalyticsVO> buildQuestionAnalytics(
            List<Map<String, Object>> questions,
            List<Submission> submissions,
            Map<Long, User> studentMap) {
        List<TaskAnalyticsVO.QuestionAnalyticsVO> rows = new ArrayList<>();
        for (int index = 0; index < questions.size(); index++) {
            Map<String, Object> question = questions.get(index);
            String questionId = String.valueOf(question.getOrDefault("id", ""));
            Object expected = question.get("answer");
            boolean autoGradable = Boolean.TRUE.equals(question.get("autoGrade")) && expected != null;
            Map<String, Integer> distribution = initialDistribution(question);
            List<TaskAnalyticsVO.StudentAnswerVO> answers = new ArrayList<>();
            int answered = 0;
            int correct = 0;

            for (Submission sub : submissions) {
                Map<String, Object> answerMap = parseAnswerMap(sub.getContent());
                Object answer = answerMap.get(questionId);
                if (hasAnswer(answer)) {
                    answered++;
                    for (String key : answerKeys(answer)) {
                        distribution.merge(key, 1, Integer::sum);
                    }
                }
                Boolean isCorrect = autoGradable ? answersEqual(expected, answer) : null;
                if (Boolean.TRUE.equals(isCorrect)) correct++;
                User student = studentMap.get(sub.getStudentId());
                answers.add(TaskAnalyticsVO.StudentAnswerVO.builder()
                        .submissionId(sub.getId())
                        .studentId(sub.getStudentId())
                        .studentName(student != null ? student.getName() : null)
                        .studentNo(student != null ? student.getStudentNo() : null)
                        .status(sub.getStatus())
                        .answer(answer)
                        .correct(isCorrect)
                        .submittedAt(sub.getSubmittedAt())
                        .build());
            }

            rows.add(TaskAnalyticsVO.QuestionAnalyticsVO.builder()
                    .questionId(questionId)
                    .index(index + 1)
                    .type(String.valueOf(question.getOrDefault("type", "")))
                    .stem(questionStem(question))
                    .autoGradable(autoGradable)
                    .answerCount(answered)
                    .correctCount(autoGradable ? correct : 0)
                    .accuracyRate(autoGradable ? percent(correct, answered) : 0)
                    .optionDistribution(distribution)
                    .answers(answers)
                    .build());
        }
        return rows;
    }

    private Map<String, Object> parseAnswerMap(String content) {
        if (content == null || content.isBlank()) return Map.of();
        try {
            return JSON.readValue(content, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<String, Integer> initialDistribution(Map<String, Object> question) {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        Object options = question.get("options");
        if (options instanceof List<?> list) {
            list.stream().map(String::valueOf).forEach(option -> distribution.put(option, 0));
        }
        if ("true_false".equals(question.get("type"))) {
            distribution.putIfAbsent("正确", 0);
            distribution.putIfAbsent("错误", 0);
        }
        return distribution;
    }

    private String questionStem(Map<String, Object> question) {
        Object stem = question.get("stem");
        if (stem instanceof String text && !text.isBlank()) return text;
        return Stream.of(question.get("title"), question.get("markdown"))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(text -> !text.isBlank())
                .collect(Collectors.joining("\n\n"));
    }

    private boolean hasAnswer(Object answer) {
        if (answer == null) return false;
        if (answer instanceof String text) return !text.isBlank();
        if (answer instanceof Collection<?> collection) return !collection.isEmpty();
        return true;
    }

    private List<String> answerKeys(Object answer) {
        if (answer instanceof Collection<?> collection) {
            return collection.stream().map(this::answerKey).toList();
        }
        return List.of(answerKey(answer));
    }

    private String answerKey(Object answer) {
        if (answer instanceof Boolean value) return value ? "正确" : "错误";
        return String.valueOf(answer);
    }

    private boolean answersEqual(Object expected, Object actual) {
        if (expected instanceof Collection<?> || actual instanceof Collection<?>) {
            Collection<?> left = expected instanceof Collection<?> collection ? collection : List.of(expected);
            Collection<?> right = actual instanceof Collection<?> collection ? collection : List.of(actual);
            return left.stream().map(String::valueOf).collect(Collectors.toSet())
                    .equals(right.stream().map(String::valueOf).collect(Collectors.toSet()));
        }
        if (expected instanceof Boolean expectedBool) {
            if (actual instanceof Boolean actualBool) return expectedBool.equals(actualBool);
            return expectedBool.toString().equalsIgnoreCase(String.valueOf(actual));
        }
        return Objects.equals(String.valueOf(expected).trim(), String.valueOf(actual).trim());
    }

    private double percent(double numerator, double denominator) {
        if (denominator <= 0) return 0;
        return round(numerator * 100 / denominator);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
