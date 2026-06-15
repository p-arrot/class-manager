package com.example.edu.modules.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.modules.course.dto.CoursePageDTO;
import com.example.edu.modules.course.entity.Lesson;
import com.example.edu.modules.course.entity.Semester;
import com.example.edu.modules.course.mapper.LessonMapper;
import com.example.edu.modules.course.mapper.SemesterMapper;
import com.example.edu.modules.course.service.CourseService;
import com.example.edu.modules.course.vo.CourseVO;
import com.example.edu.modules.dashboard.vo.StudentDashboardVO;
import com.example.edu.modules.dashboard.vo.TeacherDashboardVO;
import com.example.edu.modules.task.entity.Submission;
import com.example.edu.modules.task.entity.Task;
import com.example.edu.modules.task.mapper.SubmissionMapper;
import com.example.edu.modules.task.mapper.TaskMapper;
import com.example.edu.modules.task.vo.SubmissionVO;
import com.example.edu.modules.task.vo.TaskVO;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int STUDENT_COURSE_LIMIT = 12;
    private static final int TEACHER_COURSE_LIMIT = 50;
    private static final int STUDENT_DUE_TASK_LIMIT = 4;
    private static final int STUDENT_RECENT_GRADE_LIMIT = 3;
    private static final int TEACHER_LIST_LIMIT = 8;

    private final CourseService courseService;
    private final SemesterMapper semesterMapper;
    private final LessonMapper lessonMapper;
    private final TaskMapper taskMapper;
    private final SubmissionMapper submissionMapper;
    private final UserMapper userMapper;

    public StudentDashboardVO studentDashboard() {
        Long studentId = SecurityUtils.getCurrentUserId();
        IPage<CourseVO> coursePage = courseService.page(coursePageDTO(STUDENT_COURSE_LIMIT));
        List<CourseVO> courses = coursePage.getRecords();
        List<Long> courseIds = courses.stream().map(CourseVO::getId).toList();

        List<Semester> semesters = courseIds.isEmpty() ? List.of() : semesterMapper.selectList(new LambdaQueryWrapper<Semester>()
                .in(Semester::getCourseId, courseIds)
                .orderByDesc(Semester::getStartTime));
        Set<Long> activeSemesterIds = semesters.stream()
                .collect(Collectors.groupingBy(Semester::getCourseId))
                .values().stream()
                .map(list -> list.get(0).getId())
                .collect(Collectors.toSet());

        DashboardData data = loadData(activeSemesterIds, studentId);
        Map<Long, CourseVO> courseMap = courses.stream().collect(Collectors.toMap(CourseVO::getId, Function.identity()));
        Map<Long, Submission> mySubmissionByTask = data.submissions().stream()
                .collect(Collectors.toMap(Submission::getTaskId, Function.identity(), (a, b) -> newestSubmission(a, b)));

        List<StudentDashboardVO.DueTaskVO> dueTasks = data.tasks().stream()
                .filter(task -> task.getDeadline() != null && task.getDeadline().isAfter(LocalDateTime.now()))
                .filter(task -> {
                    Submission sub = mySubmissionByTask.get(task.getId());
                    return sub == null || !"graded".equals(sub.getStatus());
                })
                .sorted(Comparator.comparing(Task::getDeadline))
                .limit(STUDENT_DUE_TASK_LIMIT)
                .map(task -> {
                    Lesson lesson = data.lessonMap().get(task.getLessonId());
                    Semester semester = lesson != null ? data.semesterMap().get(lesson.getSemesterId()) : null;
                    CourseVO course = semester != null ? courseMap.get(semester.getCourseId()) : null;
                    return StudentDashboardVO.DueTaskVO.builder()
                            .task(toTaskVO(task))
                            .courseName(course != null ? course.getName() : "")
                            .lessonName(lesson != null ? lesson.getName() : "")
                            .courseId(course != null ? course.getId() : null)
                            .build();
                })
                .toList();

        List<StudentDashboardVO.RecentGradeVO> recentGrades = mySubmissionByTask.values().stream()
                .filter(sub -> "graded".equals(sub.getStatus()))
                .sorted((a, b) -> submitTime(b).compareTo(submitTime(a)))
                .limit(STUDENT_RECENT_GRADE_LIMIT)
                .map(sub -> {
                    Task task = data.taskMap().get(sub.getTaskId());
                    Lesson lesson = task != null ? data.lessonMap().get(task.getLessonId()) : null;
                    Semester semester = lesson != null ? data.semesterMap().get(lesson.getSemesterId()) : null;
                    CourseVO course = semester != null ? courseMap.get(semester.getCourseId()) : null;
                    return StudentDashboardVO.RecentGradeVO.builder()
                            .submission(toSubmissionVO(sub, null))
                            .taskTitle(task != null ? task.getTitle() : "")
                            .courseName(course != null ? course.getName() : "")
                            .build();
                })
                .toList();

        return StudentDashboardVO.builder()
                .courses(courses)
                .totalCourses(coursePage.getTotal())
                .dueTasks(dueTasks)
                .recentGrades(recentGrades)
                .build();
    }

    public TeacherDashboardVO teacherDashboard() {
        IPage<CourseVO> coursePage = courseService.page(coursePageDTO(TEACHER_COURSE_LIMIT));
        List<Long> courseIds = coursePage.getRecords().stream().map(CourseVO::getId).toList();
        List<Semester> semesters = courseIds.isEmpty() ? List.of() : semesterMapper.selectList(new LambdaQueryWrapper<Semester>()
                .in(Semester::getCourseId, courseIds));
        DashboardData data = loadTeacherData(semesters.stream().map(Semester::getId).collect(Collectors.toSet()));

        List<Submission> pendingSubmissionRows = loadSubmissions(data.taskIds(), "submitted", TEACHER_LIST_LIMIT);
        List<Submission> recentSubmissionRows = loadSubmissions(data.taskIds(), null, TEACHER_LIST_LIMIT);
        List<Submission> visibleSubmissionRows = combineDistinct(pendingSubmissionRows, recentSubmissionRows);
        Set<Long> studentIds = visibleSubmissionRows.stream().map(Submission::getStudentId).collect(Collectors.toSet());
        Map<Long, User> userMap = studentIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<TeacherDashboardVO.RecentSubmissionVO> pendingSubmissions = pendingSubmissionRows.stream()
                .map(sub -> toRecentSubmissionVO(sub, data, userMap))
                .toList();

        List<TeacherDashboardVO.RecentSubmissionVO> recentSubmissions = recentSubmissionRows.stream()
                .map(sub -> toRecentSubmissionVO(sub, data, userMap))
                .toList();

        List<TeacherDashboardVO.UpcomingTaskVO> upcomingTasks = data.tasks().stream()
                .filter(task -> task.getDeadline() != null && task.getDeadline().isAfter(LocalDateTime.now()))
                .sorted(Comparator.comparing(Task::getDeadline))
                .limit(TEACHER_LIST_LIMIT)
                .map(task -> {
                    Lesson lesson = data.lessonMap().get(task.getLessonId());
                    Semester semester = lesson != null ? data.semesterMap().get(lesson.getSemesterId()) : null;
                    return TeacherDashboardVO.UpcomingTaskVO.builder()
                            .task(toTaskVO(task))
                            .lessonName(lesson != null ? lesson.getName() : "")
                            .semesterName(semester != null ? semester.getName() : "")
                            .build();
                })
                .toList();

        int pending = countSubmissions(data.taskIds(), "submitted");
        int recentCount = countSubmissions(data.taskIds(), null);
        int upcomingCount = (int) data.tasks().stream()
                .filter(task -> task.getDeadline() != null && task.getDeadline().isAfter(LocalDateTime.now()))
                .count();
        return TeacherDashboardVO.builder()
                .pendingGrading(pending)
                .upcomingDeadlines(upcomingCount)
                .recentCount(recentCount)
                .pendingSubmissions(pendingSubmissions)
                .recentSubmissions(recentSubmissions)
                .upcomingTasks(upcomingTasks)
                .build();
    }

    private DashboardData loadData(Set<Long> semesterIds, Long studentId) {
        if (semesterIds == null || semesterIds.isEmpty()) {
            return new DashboardData(List.of(), List.of(), List.of(), List.of(), Map.of(), Map.of(), Map.of());
        }
        List<Semester> semesters = semesterMapper.selectBatchIds(semesterIds);
        List<Lesson> lessons = lessonMapper.selectList(new LambdaQueryWrapper<Lesson>()
                .in(Lesson::getSemesterId, semesterIds));
        List<Long> lessonIds = lessons.stream().map(Lesson::getId).toList();
        List<Task> tasks = lessonIds.isEmpty() ? List.of() : taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .in(Task::getLessonId, lessonIds));
        List<Long> taskIds = tasks.stream().map(Task::getId).toList();
        LambdaQueryWrapper<Submission> submissionQuery = new LambdaQueryWrapper<Submission>()
                .in(!taskIds.isEmpty(), Submission::getTaskId, taskIds);
        if (studentId != null) {
            submissionQuery.eq(Submission::getStudentId, studentId);
        }
        List<Submission> submissions = taskIds.isEmpty() ? List.of() : submissionMapper.selectList(submissionQuery);
        return new DashboardData(
                lessons,
                tasks,
                submissions,
                taskIds,
                semesters.stream().collect(Collectors.toMap(Semester::getId, Function.identity())),
                lessons.stream().collect(Collectors.toMap(Lesson::getId, Function.identity())),
                tasks.stream().collect(Collectors.toMap(Task::getId, Function.identity()))
        );
    }

    private DashboardData loadTeacherData(Set<Long> semesterIds) {
        if (semesterIds == null || semesterIds.isEmpty()) {
            return new DashboardData(List.of(), List.of(), List.of(), List.of(), Map.of(), Map.of(), Map.of());
        }
        List<Semester> semesters = semesterMapper.selectBatchIds(semesterIds);
        List<Lesson> lessons = lessonMapper.selectList(new LambdaQueryWrapper<Lesson>()
                .in(Lesson::getSemesterId, semesterIds));
        List<Long> lessonIds = lessons.stream().map(Lesson::getId).toList();
        List<Task> tasks = lessonIds.isEmpty() ? List.of() : taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .in(Task::getLessonId, lessonIds));
        List<Long> taskIds = tasks.stream().map(Task::getId).toList();
        return new DashboardData(
                lessons,
                tasks,
                List.of(),
                taskIds,
                semesters.stream().collect(Collectors.toMap(Semester::getId, Function.identity())),
                lessons.stream().collect(Collectors.toMap(Lesson::getId, Function.identity())),
                tasks.stream().collect(Collectors.toMap(Task::getId, Function.identity()))
        );
    }

    private List<Submission> loadSubmissions(List<Long> taskIds, String status, int limit) {
        if (taskIds == null || taskIds.isEmpty()) return List.of();
        LambdaQueryWrapper<Submission> query = new LambdaQueryWrapper<Submission>()
                .in(Submission::getTaskId, taskIds)
                .eq(status != null, Submission::getStatus, status)
                .orderByDesc(Submission::getSubmittedAt)
                .orderByDesc(Submission::getCreatedAt);
        return submissionMapper.selectPage(new Page<>(1, limit), query).getRecords();
    }

    private int countSubmissions(List<Long> taskIds, String status) {
        if (taskIds == null || taskIds.isEmpty()) return 0;
        LambdaQueryWrapper<Submission> query = new LambdaQueryWrapper<Submission>()
                .in(Submission::getTaskId, taskIds)
                .eq(status != null, Submission::getStatus, status);
        return Math.toIntExact(submissionMapper.selectCount(query));
    }

    private List<Submission> combineDistinct(Collection<Submission> first, Collection<Submission> second) {
        return java.util.stream.Stream.concat(first.stream(), second.stream())
                .collect(Collectors.toMap(Submission::getId, Function.identity(), (a, b) -> a))
                .values().stream()
                .toList();
    }

    private TeacherDashboardVO.RecentSubmissionVO toRecentSubmissionVO(
            Submission sub,
            DashboardData data,
            Map<Long, User> userMap
    ) {
        Task task = data.taskMap().get(sub.getTaskId());
        Lesson lesson = task != null ? data.lessonMap().get(task.getLessonId()) : null;
        Semester semester = lesson != null ? data.semesterMap().get(lesson.getSemesterId()) : null;
        return TeacherDashboardVO.RecentSubmissionVO.builder()
                .submission(toSubmissionVO(sub, userMap.get(sub.getStudentId())))
                .taskTitle(task != null ? task.getTitle() : "")
                .semesterName(semester != null ? semester.getName() : "")
                .build();
    }

    private CoursePageDTO coursePageDTO(int size) {
        CoursePageDTO dto = new CoursePageDTO();
        dto.setPage(1);
        dto.setSize(size);
        return dto;
    }

    private TaskVO toTaskVO(Task task) {
        return TaskVO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .type(task.getType())
                .lessonId(task.getLessonId())
                .description(task.getDescription())
                .deadline(task.getDeadline())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
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

    private static Submission newestSubmission(Submission a, Submission b) {
        return submitTime(a).isAfter(submitTime(b)) ? a : b;
    }

    private static LocalDateTime submitTime(Submission submission) {
        if (submission.getSubmittedAt() != null) return submission.getSubmittedAt();
        return submission.getCreatedAt() != null ? submission.getCreatedAt() : LocalDateTime.MIN;
    }

    private record DashboardData(
            List<Lesson> lessons,
            List<Task> tasks,
            List<Submission> submissions,
            List<Long> taskIds,
            Map<Long, Semester> semesterMap,
            Map<Long, Lesson> lessonMap,
            Map<Long, Task> taskMap
    ) {}
}
