package com.example.edu.modules.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.security.LoginUser;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.classes.entity.SchoolClass;
import com.example.edu.modules.classes.mapper.SchoolClassMapper;
import com.example.edu.modules.classes.mapper.TeacherClassMapper;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.CourseClass;
import com.example.edu.modules.course.entity.Lesson;
import com.example.edu.modules.course.entity.Semester;
import com.example.edu.modules.course.mapper.CourseClassMapper;
import com.example.edu.modules.course.mapper.CourseMapper;
import com.example.edu.modules.course.mapper.LessonMapper;
import com.example.edu.modules.course.mapper.SemesterMapper;
import com.example.edu.modules.evaluation.service.DimensionScoreService;
import com.example.edu.modules.evaluation.service.QuestionScoreHelper;
import com.example.edu.modules.realtime.service.RealtimeService;
import com.example.edu.modules.task.entity.Submission;
import com.example.edu.modules.task.entity.Task;
import com.example.edu.modules.task.mapper.SubmissionMapper;
import com.example.edu.modules.task.mapper.TaskMapper;
import com.example.edu.modules.task.service.impl.TaskServiceImpl;
import com.example.edu.modules.task.vo.TaskAnalyticsVO;
import com.example.edu.modules.task.vo.TaskResultVO;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock private TaskMapper taskMapper;
    @Mock private SubmissionMapper submissionMapper;
    @Mock private LessonMapper lessonMapper;
    @Mock private SemesterMapper semesterMapper;
    @Mock private CourseMapper courseMapper;
    @Mock private TeacherClassMapper teacherClassMapper;
    @Mock private CourseClassMapper courseClassMapper;
    @Mock private SchoolClassMapper schoolClassMapper;
    @Mock private UserMapper userMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private RealtimeService realtimeService;
    @Mock private DimensionScoreService dimensionScoreService;
    @Mock private QuestionScoreHelper questionScoreHelper;
    @Mock private TaskResultAssembler taskResultAssembler;

    @InjectMocks
    private TaskServiceImpl service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void analyticsCalculatesSubmissionRateAccuracyAndQuestionAnswers() {
        setTeacher();
        Task task = task();
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(lessonMapper.selectById(2L)).thenReturn(lesson());
        when(semesterMapper.selectById(3L)).thenReturn(semester());
        when(courseMapper.selectById(4L)).thenReturn(course());
        when(courseClassMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(courseClass(10L)));
        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(student(101L, "2026101", "林一"), student(102L, "2026102", "周二")));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                submission(11L, 101L, "{\"q1\":\"A\",\"q2\":[\"B\",\"C\"]}", "graded"),
                submission(12L, 102L, "{\"q1\":\"B\",\"q2\":[\"C\",\"B\"]}", "submitted")
        ));

        TaskAnalyticsVO analytics = service.getTaskAnalytics(1L, null);

        assertThat(analytics.getTotalStudents()).isEqualTo(2);
        assertThat(analytics.getSubmittedCount()).isEqualTo(1);
        assertThat(analytics.getGradedCount()).isEqualTo(1);
        assertThat(analytics.getSpecialCount()).isZero();
        assertThat(analytics.getNotSubmittedCount()).isZero();
        assertThat(analytics.getSubmissionRate()).isEqualTo(100.0);
        assertThat(analytics.getAccuracyRate()).isEqualTo(75.0);
        assertThat(analytics.getQuestionCount()).isEqualTo(2);
        assertThat(analytics.getAutoQuestionCount()).isEqualTo(2);
        assertThat(analytics.getManualQuestionCount()).isZero();
        assertThat(analytics.getQuestions()).hasSize(2);
        assertThat(analytics.getQuestions().get(0).getAccuracyRate()).isEqualTo(50.0);
        assertThat(analytics.getQuestions().get(0).getOptionDistribution()).containsEntry("A", 1).containsEntry("B", 1);
        assertThat(analytics.getQuestions().get(1).getAccuracyRate()).isEqualTo(100.0);
        assertThat(analytics.getQuestions().get(1).getAnswers()).extracting(TaskAnalyticsVO.StudentAnswerVO::getStudentName)
                .containsExactly("林一", "周二");
    }

    @Test
    void analyticsIncludesLegacyWorksheetFields() {
        setTeacher();
        Task task = task();
        task.setFormSchema("""
                {
                  "version": 1,
                  "fields": [
                    {"id": "q1", "type": "radio", "label": "旧版单选题", "options": ["A", "B"]}
                  ]
                }
                """);
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(lessonMapper.selectById(2L)).thenReturn(lesson());
        when(semesterMapper.selectById(3L)).thenReturn(semester());
        when(courseMapper.selectById(4L)).thenReturn(course());
        when(courseClassMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(courseClass(10L)));
        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(student(101L, "2026101", "林一")));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                submission(11L, 101L, "{\"q1\":\"A\"}", "submitted")
        ));

        TaskAnalyticsVO analytics = service.getTaskAnalytics(1L, null);

        assertThat(analytics.getQuestions()).hasSize(1);
        assertThat(analytics.getQuestions().get(0).getType()).isEqualTo("single");
        assertThat(analytics.getQuestions().get(0).getStem()).isEqualTo("旧版单选题");
        assertThat(analytics.getQuestions().get(0).getAnswerCount()).isEqualTo(1);
    }

    @Test
    void analyticsIncludesNotSubmittedStudentsInInboxRows() {
        setTeacher();
        Task task = task();
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(lessonMapper.selectById(2L)).thenReturn(lesson());
        when(semesterMapper.selectById(3L)).thenReturn(semester());
        when(courseMapper.selectById(4L)).thenReturn(course());
        when(courseClassMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(courseClass(10L)));
        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                student(101L, "2026101", "林一"),
                student(102L, "2026102", "周二")
        ));
        when(schoolClassMapper.selectBatchIds(any())).thenReturn(List.of(schoolClass(10L)));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                submission(11L, 101L, "{\"q1\":\"A\"}", "submitted")
        ));

        TaskAnalyticsVO analytics = service.getTaskAnalytics(1L, null);

        assertThat(analytics.getTotalStudents()).isEqualTo(2);
        assertThat(analytics.getSubmittedCount()).isEqualTo(1);
        assertThat(analytics.getNotSubmittedCount()).isEqualTo(1);
        assertThat(analytics.getSubmissions()).hasSize(2);
        assertThat(analytics.getSubmissions()).extracting(TaskAnalyticsVO.StudentTaskAnswerVO::getStatus)
                .containsExactly("submitted", "not_submitted");
        assertThat(analytics.getSubmissions()).extracting(TaskAnalyticsVO.StudentTaskAnswerVO::getClassName)
                .containsExactly("2026级1班", "2026级1班");
        assertThat(analytics.getSubmissions().get(1).getSubmissionId()).isNull();
        assertThat(analytics.getClassScopes()).hasSize(1);
        assertThat(analytics.getClassScopes().get(0).getStudentCount()).isEqualTo(2);
    }

    @Test
    void analyticsToleratesQuestionWithoutStemTitleOrMarkdown() {
        setTeacher();
        Task task = task();
        task.setFormSchema("""
                {
                  "version": 3,
                  "questions": [
                    {"id": "q1", "type": "short_answer", "autoGrade": false}
                  ]
                }
                """);
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(lessonMapper.selectById(2L)).thenReturn(lesson());
        when(semesterMapper.selectById(3L)).thenReturn(semester());
        when(courseMapper.selectById(4L)).thenReturn(course());
        when(courseClassMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(courseClass(10L)));
        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(student(101L, "2026101", "林一")));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                submission(11L, 101L, "{\"q1\":\"answer\"}", "submitted")
        ));

        TaskAnalyticsVO analytics = service.getTaskAnalytics(1L, null);

        assertThat(analytics.getQuestions()).hasSize(1);
        assertThat(analytics.getQuestions().get(0).getStem()).isEmpty();
        assertThat(analytics.getQuestions().get(0).getAnswerCount()).isEqualTo(1);
    }

    @Test
    void getMyResultChecksAccessAndDelegatesToAssembler() {
        setStudent(101L);
        Task task = task();
        TaskResultVO expected = TaskResultVO.builder().status("graded").build();

        when(taskMapper.selectById(1L)).thenReturn(task);
        when(lessonMapper.selectById(2L)).thenReturn(lesson());
        when(semesterMapper.selectById(3L)).thenReturn(semester());
        when(courseMapper.selectById(4L)).thenReturn(course());
        when(courseClassMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(taskResultAssembler.build(task, 101L)).thenReturn(expected);

        TaskResultVO result = service.getMyResult(1L, 101L);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void submitKeepsWorksheetSubmittedWhenManualQuestionsRemain() {
        setStudent(101L);
        Task task = taskWithManualQuestion();
        Submission existing = submission(11L, 101L, "{\"q1\":\"A\"}", "submitted");

        when(taskMapper.selectById(1L)).thenReturn(task);
        when(lessonMapper.selectById(2L)).thenReturn(lesson());
        when(semesterMapper.selectById(3L)).thenReturn(semester());
        when(courseMapper.selectById(4L)).thenReturn(course());
        when(courseClassMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(submissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(questionScoreHelper.autoGrade(any(), any())).thenReturn(List.of(
                new DimensionScoreService.ScoreInput("q1", "COMPUTING", java.math.BigDecimal.ONE, java.math.BigDecimal.ONE, true)
        ));

        service.submit(1L, submissionDto("{\"q1\":\"A\",\"q2\":\"需要老师看\"}"));

        ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionMapper, atLeastOnce()).updateById(captor.capture());
        assertThat(captor.getAllValues()).extracting(Submission::getStatus)
                .containsOnly("submitted");
    }

    @Test
    void submitMarksWorksheetGradedWhenAllQuestionsAreAutoGradable() {
        setStudent(101L);
        Task task = task();
        Submission existing = submission(11L, 101L, "{\"q1\":\"A\"}", "submitted");

        when(taskMapper.selectById(1L)).thenReturn(task);
        when(lessonMapper.selectById(2L)).thenReturn(lesson());
        when(semesterMapper.selectById(3L)).thenReturn(semester());
        when(courseMapper.selectById(4L)).thenReturn(course());
        when(courseClassMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(submissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(questionScoreHelper.autoGrade(any(), any())).thenReturn(List.of(
                new DimensionScoreService.ScoreInput("q1", "COMPUTING", java.math.BigDecimal.ONE, java.math.BigDecimal.ONE, true)
        ));

        service.submit(1L, submissionDto("{\"q1\":\"A\",\"q2\":[\"B\",\"C\"]}"));

        ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionMapper, atLeastOnce()).updateById(captor.capture());
        assertThat(captor.getAllValues()).extracting(Submission::getStatus)
                .contains("graded");
    }

    @Test
    void studentCannotReadAnotherStudentsSubmissionById() {
        setStudent(102L);
        when(submissionMapper.selectById(11L)).thenReturn(submission(11L, 101L, "{\"q1\":\"A\"}", "graded"));
        when(taskMapper.selectById(1L)).thenReturn(task());

        assertThatThrownBy(() -> service.getSubmission(11L))
                .hasMessageContaining("无权操作该课程");
    }

    private static void setTeacher() {
        LoginUser loginUser = new LoginUser(9L, "teacher01", "teacher", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    private static void setStudent(Long id) {
        LoginUser loginUser = new LoginUser(id, "student" + id, "student", 10L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    private static Task task() {
        Task task = new Task();
        task.setId(1L);
        task.setLessonId(2L);
        task.setTitle("课堂练习");
        task.setType("worksheet");
        task.setFormSchema("""
                {
                  "version": 3,
                  "questions": [
                    {"id": "q1", "type": "single", "stem": "选择 A", "autoGrade": true, "answer": "A", "options": ["A", "B"]},
                    {"id": "q2", "type": "multiple", "stem": "选择 B 和 C", "autoGrade": true, "answer": ["B", "C"], "options": ["A", "B", "C"]}
                  ]
                }
                """);
        return task;
    }

    private static Task taskWithManualQuestion() {
        Task task = task();
        task.setFormSchema("""
                {
                  "version": 3,
                  "questions": [
                    {"id": "q1", "type": "single", "stem": "选择 A", "autoGrade": true, "answer": "A", "options": ["A", "B"]},
                    {"id": "q2", "type": "short", "stem": "说明原因", "autoGrade": false}
                  ]
                }
                """);
        return task;
    }

    private static Lesson lesson() {
        Lesson lesson = new Lesson();
        lesson.setId(2L);
        lesson.setSemesterId(3L);
        return lesson;
    }

    private static Semester semester() {
        Semester semester = new Semester();
        semester.setId(3L);
        semester.setCourseId(4L);
        return semester;
    }

    private static Course course() {
        Course course = new Course();
        course.setId(4L);
        course.setTeacherId(9L);
        return course;
    }

    private static CourseClass courseClass(Long classId) {
        CourseClass courseClass = new CourseClass();
        courseClass.setCourseId(4L);
        courseClass.setClassId(classId);
        return courseClass;
    }

    private static User student(Long id, String studentNo, String name) {
        User user = new User();
        user.setId(id);
        user.setStudentNo(studentNo);
        user.setName(name);
        user.setRole("student");
        user.setClassId(10L);
        return user;
    }

    private static SchoolClass schoolClass(Long id) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(id);
        schoolClass.setGrade("2026");
        schoolClass.setName("1班");
        return schoolClass;
    }

    private static Submission submission(Long id, Long studentId, String content, String status) {
        Submission submission = new Submission();
        submission.setId(id);
        submission.setTaskId(1L);
        submission.setStudentId(studentId);
        submission.setContent(content);
        submission.setStatus(status);
        submission.setSubmittedAt(LocalDateTime.now());
        return submission;
    }

    private static com.example.edu.modules.task.dto.SubmissionDTO submissionDto(String content) {
        com.example.edu.modules.task.dto.SubmissionDTO dto = new com.example.edu.modules.task.dto.SubmissionDTO();
        dto.setContent(content);
        return dto;
    }

}
