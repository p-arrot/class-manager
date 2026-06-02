package com.example.edu.modules.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.Lesson;
import com.example.edu.modules.course.entity.Semester;
import com.example.edu.modules.course.mapper.LessonMapper;
import com.example.edu.modules.course.mapper.SemesterMapper;
import com.example.edu.modules.course.mapper.CourseMapper;
import com.example.edu.modules.course.service.CoursePermissionHelper;
import com.example.edu.modules.classes.entity.TeacherClass;
import com.example.edu.modules.classes.mapper.TeacherClassMapper;
import com.example.edu.modules.course.mapper.CourseClassMapper;
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
import com.example.edu.modules.task.vo.TaskDetailVO;
import com.example.edu.modules.task.vo.TaskVO;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
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
public class TaskServiceImpl implements TaskService {

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
            task.setDeadline(LocalDateTime.parse(dto.getDeadline()));
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
        if (dto.getDeadline() != null) task.setDeadline(LocalDateTime.parse(dto.getDeadline()));
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
}
