package com.example.edu.modules.stats.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.LoginUser;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.classes.entity.SchoolClass;
import com.example.edu.modules.classes.mapper.SchoolClassMapper;
import com.example.edu.modules.course.entity.AssessmentScheme;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.CourseClass;
import com.example.edu.modules.course.entity.Lesson;
import com.example.edu.modules.course.entity.Semester;
import com.example.edu.modules.course.mapper.AssessmentSchemeMapper;
import com.example.edu.modules.course.mapper.CourseClassMapper;
import com.example.edu.modules.course.mapper.CourseMapper;
import com.example.edu.modules.course.mapper.LessonMapper;
import com.example.edu.modules.course.mapper.SemesterMapper;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock private DimensionScoreMapper dimensionScoreMapper;
    @Mock private ExamSubmissionMapper examSubmissionMapper;
    @Mock private ExamMapper examMapper;
    @Mock private ProjectSubmissionMapper projectSubmissionMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private SubmissionMapper submissionMapper;
    @Mock private TaskMapper taskMapper;
    @Mock private LessonMapper lessonMapper;
    @Mock private SemesterMapper semesterMapper;
    @Mock private CourseMapper courseMapper;
    @Mock private AssessmentSchemeMapper assessmentSchemeMapper;
    @Mock private UserMapper userMapper;
    @Mock private SchoolClassMapper schoolClassMapper;
    @Mock private CourseClassMapper courseClassMapper;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private StatsService statsService;

    private static final Long SEMESTER_ID = 1L;
    private static final Long STUDENT_ID = 100L;
    private static final Long CLASS_ID = 10L;

    private Lesson lesson;
    private Task task;
    private Submission taskSubmission;
    private User student;
    private SchoolClass schoolClass;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        student = new User();
        student.setId(STUDENT_ID);
        student.setName("测试学生");
        student.setStudentNo("2026001");
        student.setClassId(CLASS_ID);

        schoolClass = new SchoolClass();
        schoolClass.setId(CLASS_ID);
        schoolClass.setGrade("2026");
        schoolClass.setName("1班");

        lesson = new Lesson();
        lesson.setId(1L);
        lesson.setSemesterId(SEMESTER_ID);

        task = new Task();
        task.setId(1L);
        task.setLessonId(1L);
        task.setType("worksheet");

        taskSubmission = new Submission();
        taskSubmission.setId(11L);
        taskSubmission.setTaskId(1L);
        taskSubmission.setStudentId(STUDENT_ID);
        taskSubmission.setStatus("graded");
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noScoresReturnsEmptyList() {
        setAdmin();
        semesterBelongsToTeacher(9L);
        when(lessonMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(examMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(projectMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertThat(statsService.calculateSemesterGrades(SEMESTER_ID)).isEmpty();
    }

    @Test
    void courseRosterStudentWithoutSubmissionsStillAppearsInExport() {
        setAdmin();
        semesterBelongsToTeacher(9L);
        when(lessonMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(examMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(projectMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        baseUsers();

        List<StatsService.GradeRow> rows = statsService.calculateSemesterGrades(SEMESTER_ID);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).studentId()).isEqualTo(STUDENT_ID);
        assertThat(rows.get(0).totalGrade()).isEqualTo("暂无数据");
    }

    @Test
    void processScoresCalculateDimensionAndTotal() {
        setAdmin();
        semesterBelongsToTeacher(9L);
        baseTaskData();
        when(dimensionScoreMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                score("process", 11L, "AWARENESS", 8, 10),
                score("process", 11L, "COMPUTING", 5, 10)
        ));
        when(examMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(projectMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        baseUsers();

        List<StatsService.GradeRow> rows = statsService.calculateSemesterGrades(SEMESTER_ID);

        assertThat(rows).hasSize(1);
        StatsService.GradeRow row = rows.get(0);
        assertThat(row.processScore()).isEqualTo(65.0);
        assertThat(row.awareness()).isEqualTo(80);
        assertThat(row.computing()).isEqualTo(50);
        assertThat(row.totalScore()).isEqualTo(65.0);
        assertThat(row.remark()).isEqualTo("缺考试成绩");
    }

    @Test
    void multipleExamsPoolByDimensionInsideExamBucket() {
        setAdmin();
        semesterBelongsToTeacher(9L);
        baseTaskData();
        when(dimensionScoreMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(
                List.of(score("process", 11L, "COMPUTING", 10, 10)),
                List.of(
                        score("exam", 21L, "COMPUTING", 8, 10),
                        score("exam", 22L, "COMPUTING", 6, 10)
                ),
                List.of()
        );

        Exam exam1 = new Exam();
        exam1.setId(1L);
        Exam exam2 = new Exam();
        exam2.setId(2L);
        when(examMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(exam1, exam2));

        ExamSubmission sub1 = new ExamSubmission();
        sub1.setId(21L);
        sub1.setExamId(1L);
        sub1.setStudentId(STUDENT_ID);
        ExamSubmission sub2 = new ExamSubmission();
        sub2.setId(22L);
        sub2.setExamId(2L);
        sub2.setStudentId(STUDENT_ID);
        when(examSubmissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(sub1, sub2));

        when(projectMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        baseUsers();

        StatsService.GradeRow row = statsService.calculateSemesterGrades(SEMESTER_ID).get(0);

        assertThat(row.examScore()).isEqualTo(70.0);
        assertThat(row.totalScore()).isEqualTo(85.0);
        assertThat(row.computing()).isEqualTo(85);
        assertThat(row.remark()).isEmpty();
    }

    @Test
    void projectScoresAreReadFromProjectSubmissionSourceIds() {
        setAdmin();
        semesterBelongsToTeacher(9L);
        when(lessonMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(examMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        Project project = new Project();
        project.setId(31L);
        when(projectMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(project));
        ProjectSubmission projectSubmission = new ProjectSubmission();
        projectSubmission.setId(41L);
        projectSubmission.setProjectId(31L);
        projectSubmission.setStudentId(STUDENT_ID);
        when(projectSubmissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(projectSubmission));

        when(dimensionScoreMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(score("project", 41L, "DIGITAL_LEARNING", 9, 10)));
        AssessmentScheme scheme = new AssessmentScheme();
        scheme.setSemesterId(SEMESTER_ID);
        scheme.setProcessPercent(0);
        scheme.setExamPercent(0);
        scheme.setProjectPercent(100);
        when(assessmentSchemeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(scheme);
        baseUsers();

        StatsService.GradeRow row = statsService.calculateSemesterGrades(SEMESTER_ID).get(0);

        assertThat(row.projectScore()).isEqualTo(90.0);
        assertThat(row.digitalLearn()).isEqualTo(90);
        assertThat(row.totalScore()).isEqualTo(90.0);
        assertThat(row.remark()).isEmpty();
    }

    @Test
    void nonOwnerTeacherCannotPreviewSemesterGrades() {
        setTeacher(8L);
        semesterBelongsToTeacher(9L);

        assertThatThrownBy(() -> statsService.calculateSemesterGrades(SEMESTER_ID))
                .isInstanceOf(BizException.class)
                .hasMessage(ErrorCode.COURSE_ACCESS_DENIED.getMsg());
    }

    @Test
    void adminCanPreviewSemesterGradesRegardlessOfCourseTeacher() {
        setAdmin();
        semesterBelongsToTeacher(9L);
        when(lessonMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(examMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(projectMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertThat(statsService.calculateSemesterGrades(SEMESTER_ID)).isEmpty();
    }

    private void baseTaskData() {
        when(lessonMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(lesson));
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(taskSubmission));
    }

    private void baseUsers() {
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(student));
        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(student));
        when(schoolClassMapper.selectBatchIds(anyCollection())).thenReturn(List.of(schoolClass));
        CourseClass binding = new CourseClass();
        binding.setCourseId(20L);
        binding.setClassId(CLASS_ID);
        when(courseClassMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(binding));
    }

    private void semesterBelongsToTeacher(Long teacherId) {
        Semester semester = new Semester();
        semester.setId(SEMESTER_ID);
        semester.setCourseId(20L);
        when(semesterMapper.selectById(SEMESTER_ID)).thenReturn(semester);

        Course course = new Course();
        course.setId(20L);
        course.setTeacherId(teacherId);
        when(courseMapper.selectById(20L)).thenReturn(course);
    }

    private static void setAdmin() {
        LoginUser loginUser = new LoginUser(1L, "admin", "admin", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    private static void setTeacher(Long id) {
        LoginUser loginUser = new LoginUser(id, "teacher" + id, "teacher", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    private DimensionScore score(String sourceType, Long sourceId, String dimension, int earned, int max) {
        DimensionScore score = new DimensionScore();
        score.setStudentId(STUDENT_ID);
        score.setSourceType(sourceType);
        score.setSourceId(sourceId);
        score.setDimension(dimension);
        score.setEarnedScore(BigDecimal.valueOf(earned));
        score.setMaxScore(BigDecimal.valueOf(max));
        return score;
    }
}
