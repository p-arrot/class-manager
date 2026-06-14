package com.example.edu.modules.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edu.modules.course.dto.CoursePageDTO;
import com.example.edu.modules.course.entity.Lesson;
import com.example.edu.modules.course.entity.Semester;
import com.example.edu.modules.course.mapper.LessonMapper;
import com.example.edu.modules.course.mapper.SemesterMapper;
import com.example.edu.modules.course.service.CourseService;
import com.example.edu.modules.course.vo.CourseVO;
import com.example.edu.modules.dashboard.vo.TeacherDashboardVO;
import com.example.edu.modules.task.entity.Submission;
import com.example.edu.modules.task.entity.Task;
import com.example.edu.modules.task.mapper.SubmissionMapper;
import com.example.edu.modules.task.mapper.TaskMapper;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private CourseService courseService;
    @Mock private SemesterMapper semesterMapper;
    @Mock private LessonMapper lessonMapper;
    @Mock private TaskMapper taskMapper;
    @Mock private SubmissionMapper submissionMapper;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void teacherDashboardUsesPagedSubmissionsAndCountQueries() {
        Page<CourseVO> coursePage = new Page<>(1, 50);
        coursePage.setRecords(List.of(CourseVO.builder().id(4L).name("Python").build()));
        coursePage.setTotal(1);
        when(courseService.page(any(CoursePageDTO.class))).thenReturn(coursePage);

        Semester semester = semester();
        Lesson lesson = lesson();
        Task submittedTask = task(21L, "待批改任务", LocalDateTime.now().plusDays(1));
        Task recentTask = task(22L, "最近提交任务", LocalDateTime.now().plusDays(2));
        Submission pendingSubmission = submission(101L, 21L, 301L, "submitted", LocalDateTime.now().minusMinutes(5));
        Submission recentSubmission = submission(102L, 22L, 302L, "graded", LocalDateTime.now().minusMinutes(10));

        when(semesterMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(semester));
        when(semesterMapper.selectBatchIds(any())).thenReturn(List.of(semester));
        when(lessonMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(lesson));
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(submittedTask, recentTask));
        when(submissionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(pageOf(pendingSubmission), pageOf(recentSubmission));
        when(submissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(12L, 30L);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user(301L, "林一"), user(302L, "周二")));

        TeacherDashboardVO dashboard = dashboardService.teacherDashboard();

        assertThat(dashboard.getPendingGrading()).isEqualTo(12);
        assertThat(dashboard.getRecentCount()).isEqualTo(30);
        assertThat(dashboard.getPendingSubmissions()).hasSize(1);
        assertThat(dashboard.getPendingSubmissions().get(0).getTaskTitle()).isEqualTo("待批改任务");
        assertThat(dashboard.getRecentSubmissions()).hasSize(1);
        assertThat(dashboard.getRecentSubmissions().get(0).getTaskTitle()).isEqualTo("最近提交任务");
        assertThat(dashboard.getUpcomingTasks()).hasSize(2);

        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(submissionMapper, times(2)).selectPage(pageCaptor.capture(), any(LambdaQueryWrapper.class));
        assertThat(pageCaptor.getAllValues())
                .extracting(Page::getSize)
                .containsExactly(8L, 8L);
        verify(submissionMapper, times(2)).selectCount(any(LambdaQueryWrapper.class));
        verify(submissionMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    private static Page<Submission> pageOf(Submission submission) {
        Page<Submission> page = new Page<>(1, 8);
        page.setRecords(List.of(submission));
        page.setTotal(1);
        return page;
    }

    private static Semester semester() {
        Semester semester = new Semester();
        semester.setId(3L);
        semester.setCourseId(4L);
        semester.setName("2026 春");
        return semester;
    }

    private static Lesson lesson() {
        Lesson lesson = new Lesson();
        lesson.setId(11L);
        lesson.setSemesterId(3L);
        lesson.setName("条件判断");
        return lesson;
    }

    private static Task task(Long id, String title, LocalDateTime deadline) {
        Task task = new Task();
        task.setId(id);
        task.setLessonId(11L);
        task.setTitle(title);
        task.setType("worksheet");
        task.setDeadline(deadline);
        return task;
    }

    private static Submission submission(Long id, Long taskId, Long studentId, String status, LocalDateTime submittedAt) {
        Submission submission = new Submission();
        submission.setId(id);
        submission.setTaskId(taskId);
        submission.setStudentId(studentId);
        submission.setStatus(status);
        submission.setSubmittedAt(submittedAt);
        submission.setContent("{}");
        return submission;
    }

    private static User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setStudentNo("2026" + id);
        return user;
    }
}
