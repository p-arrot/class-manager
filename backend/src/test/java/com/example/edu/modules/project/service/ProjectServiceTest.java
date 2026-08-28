package com.example.edu.modules.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.LoginUser;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.classes.entity.SchoolClass;
import com.example.edu.modules.classes.mapper.SchoolClassMapper;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.CourseClass;
import com.example.edu.modules.course.entity.Semester;
import com.example.edu.modules.course.mapper.CourseClassMapper;
import com.example.edu.modules.course.mapper.CourseMapper;
import com.example.edu.modules.course.mapper.SemesterMapper;
import com.example.edu.modules.course.service.CourseRosterService;
import com.example.edu.modules.course.service.CourseRosterService.CourseRoster;
import com.example.edu.modules.evaluation.entity.DimensionScore;
import com.example.edu.modules.evaluation.service.DimensionScoreService;
import com.example.edu.modules.project.entity.Project;
import com.example.edu.modules.project.entity.ProjectSubmission;
import com.example.edu.modules.project.mapper.ProjectMapper;
import com.example.edu.modules.project.mapper.ProjectSubmissionMapper;
import com.example.edu.modules.project.vo.ProjectSubmissionVO;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock private ProjectMapper projectMapper;
    @Mock private ProjectSubmissionMapper submissionMapper;
    @Mock private SemesterMapper semesterMapper;
    @Mock private CourseMapper courseMapper;
    @Mock private CourseClassMapper courseClassMapper;
    @Mock private CourseRosterService courseRosterService;
    @Mock private AuditLogService auditLogService;
    @Mock private DimensionScoreService dimensionScoreService;

    @InjectMocks
    private ProjectService projectService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void studentInCourseClassCanSubmitProject() {
        setStudent(101L, 10L);
        Project project = project(LocalDateTime.now().plusDays(1));
        when(projectMapper.selectById(1L)).thenReturn(project);
        when(semesterMapper.selectById(3L)).thenReturn(semester());
        when(courseMapper.selectById(4L)).thenReturn(course(9L));
        when(courseClassMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(submissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        ProjectSubmission submission = projectService.submit(1L, "{\"files\":[11]}");

        assertThat(submission.getProjectId()).isEqualTo(1L);
        assertThat(submission.getStudentId()).isEqualTo(101L);
        assertThat(submission.getContent()).isEqualTo("{\"files\":[11]}");
        assertThat(submission.getSubmittedAt()).isNotNull();
        verify(submissionMapper).insert(submission);
    }

    @Test
    void studentOutsideCourseClassCannotSubmitProject() {
        setStudent(101L, 10L);
        when(projectMapper.selectById(1L)).thenReturn(project(LocalDateTime.now().plusDays(1)));
        when(semesterMapper.selectById(3L)).thenReturn(semester());
        when(courseMapper.selectById(4L)).thenReturn(course(9L));
        when(courseClassMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertThatThrownBy(() -> projectService.submit(1L, "{}"))
                .isInstanceOf(BizException.class)
                .hasMessage(ErrorCode.COURSE_ACCESS_DENIED.getMsg());
        verify(submissionMapper, never()).insert(any(ProjectSubmission.class));
    }

    @Test
    void nonOwnerTeacherCannotListProjectSubmissions() {
        setTeacher(8L);
        when(projectMapper.selectById(1L)).thenReturn(project(LocalDateTime.now().plusDays(1)));
        when(semesterMapper.selectById(3L)).thenReturn(semester());
        when(courseMapper.selectById(4L)).thenReturn(course(9L));

        assertThatThrownBy(() -> projectService.listSubmissions(1L))
                .isInstanceOf(BizException.class)
                .hasMessage(ErrorCode.COURSE_ACCESS_DENIED.getMsg());
        verify(submissionMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void ownerTeacherCanScoreProjectSubmissionWithDimensionScores() {
        setTeacher(9L);
        ProjectSubmission submission = submission(31L, 1L, 101L);
        when(submissionMapper.selectById(31L)).thenReturn(submission);
        when(projectMapper.selectById(1L)).thenReturn(project(LocalDateTime.now().plusDays(1)));
        when(semesterMapper.selectById(3L)).thenReturn(semester());
        when(courseMapper.selectById(4L)).thenReturn(course(9L));
        List<DimensionScoreService.ScoreInput> scores = List.of(
                new DimensionScoreService.ScoreInput("rubric-1", "COMPUTING", BigDecimal.valueOf(8), BigDecimal.TEN, false)
        );

        projectService.scoreSubmission(31L, scores);

        verify(dimensionScoreService).replaceScores("project", 31L, 101L, scores);
    }

    @Test
    void ownerTeacherCanListProjectSubmissionInboxWithNotSubmittedAndGradedStudents() {
        setTeacher(9L);
        when(projectMapper.selectById(1L)).thenReturn(project(LocalDateTime.now().plusDays(1)));
        when(semesterMapper.selectById(3L)).thenReturn(semester());
        when(courseMapper.selectById(4L)).thenReturn(course(9L));
        when(courseRosterService.load(4L)).thenReturn(roster(
                student(102L, "20260002", "周二"),
                student(101L, "20260001", "林一")
        ));
        ProjectSubmission gradedSubmission = submission(31L, 1L, 101L);
        gradedSubmission.setStatus("graded");
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(gradedSubmission));
        DimensionScore score = new DimensionScore();
        score.setSourceType("project");
        score.setSourceId(31L);
        score.setQuestionId("project");
        score.setDimension("COMPUTING");
        score.setEarnedScore(BigDecimal.valueOf(8));
        score.setMaxScore(BigDecimal.TEN);
        when(dimensionScoreService.listBySources("project", List.of(31L))).thenReturn(List.of(score));

        List<ProjectSubmissionVO> inbox = projectService.listSubmissionInbox(1L);

        assertThat(inbox).hasSize(2);
        ProjectSubmissionVO graded = inbox.stream().filter(row -> row.getStudentId().equals(101L)).findFirst().orElseThrow();
        assertThat(graded.getId()).isEqualTo(31L);
        assertThat(graded.getSubmissionId()).isEqualTo(31L);
        assertThat(graded.getStatus()).isEqualTo("graded");
        assertThat(graded.getScore()).isEqualByComparingTo("8");
        assertThat(graded.getDimensionScores()).singleElement().satisfies(detail -> {
            assertThat(detail.dimension()).isEqualTo("COMPUTING");
            assertThat(detail.earnedScore()).isEqualByComparingTo("8");
            assertThat(detail.maxScore()).isEqualByComparingTo("10");
        });
        assertThat(graded.getClassName()).isEqualTo("2026级1班");

        ProjectSubmissionVO notSubmitted = inbox.stream().filter(row -> row.getStudentId().equals(102L)).findFirst().orElseThrow();
        assertThat(notSubmitted.getId()).isNull();
        assertThat(notSubmitted.getSubmissionId()).isNull();
        assertThat(notSubmitted.getStatus()).isEqualTo("not_submitted");
        assertThat(notSubmitted.getClassName()).isEqualTo("2026级1班");
    }

    @Test
    void nonOwnerTeacherCannotScoreProjectSubmission() {
        setTeacher(8L);
        when(submissionMapper.selectById(31L)).thenReturn(submission(31L, 1L, 101L));
        when(projectMapper.selectById(1L)).thenReturn(project(LocalDateTime.now().plusDays(1)));
        when(semesterMapper.selectById(3L)).thenReturn(semester());
        when(courseMapper.selectById(4L)).thenReturn(course(9L));

        assertThatThrownBy(() -> projectService.scoreSubmission(31L, List.of()))
                .isInstanceOf(BizException.class)
                .hasMessage(ErrorCode.COURSE_ACCESS_DENIED.getMsg());
        verify(dimensionScoreService, never()).replaceScores(any(), any(), any(), any());
    }

    @Test
    void existingSubmissionIsUpdatedInsteadOfDuplicated() {
        setStudent(101L, 10L);
        ProjectSubmission existing = submission(31L, 1L, 101L);
        when(projectMapper.selectById(1L)).thenReturn(project(LocalDateTime.now().plusDays(1)));
        when(semesterMapper.selectById(3L)).thenReturn(semester());
        when(courseMapper.selectById(4L)).thenReturn(course(9L));
        when(courseClassMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(submissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        projectService.submit(1L, "{\"files\":[12]}");

        ArgumentCaptor<ProjectSubmission> captor = ArgumentCaptor.forClass(ProjectSubmission.class);
        verify(submissionMapper).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(31L);
        assertThat(captor.getValue().getContent()).isEqualTo("{\"files\":[12]}");
        verify(submissionMapper, never()).insert(any(ProjectSubmission.class));
    }

    @Test
    void gradedProjectCannotBeOverwrittenByStudent() {
        setStudent(101L, 10L);
        ProjectSubmission existing = submission(31L, 1L, 101L);
        existing.setStatus("graded");
        when(projectMapper.selectById(1L)).thenReturn(project(LocalDateTime.now().plusDays(1)));
        when(semesterMapper.selectById(3L)).thenReturn(semester());
        when(courseMapper.selectById(4L)).thenReturn(course(9L));
        when(courseClassMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(submissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        assertThatThrownBy(() -> projectService.submit(1L, "changed"))
                .isInstanceOf(BizException.class)
                .hasMessage(ErrorCode.SUBMISSION_LOCKED.getMsg());
        verify(submissionMapper, never()).updateById(existing);
    }

    @Test
    void ownerTeacherReturnsProjectAndClearsScores() {
        setTeacher(9L);
        ProjectSubmission existing = submission(31L, 1L, 101L);
        existing.setStatus("graded");
        when(submissionMapper.selectById(31L)).thenReturn(existing);
        when(projectMapper.selectById(1L)).thenReturn(project(LocalDateTime.now().plusDays(1)));
        when(semesterMapper.selectById(3L)).thenReturn(semester());
        when(courseMapper.selectById(4L)).thenReturn(course(9L));

        projectService.returnSubmission(31L, "请补充演示视频");

        assertThat(existing.getStatus()).isEqualTo("returned");
        assertThat(existing.getReturnReason()).isEqualTo("请补充演示视频");
        verify(dimensionScoreService).clearScores("project", 31L);
    }

    @Test
    void studentCanReadOwnProjectScoreDetailsButNotAnotherStudents() {
        setStudent(101L, 10L);
        ProjectSubmission own = submission(31L, 1L, 101L);
        when(submissionMapper.selectById(31L)).thenReturn(own);
        when(projectMapper.selectById(1L)).thenReturn(project(LocalDateTime.now().plusDays(1)));
        when(semesterMapper.selectById(3L)).thenReturn(semester());
        when(courseMapper.selectById(4L)).thenReturn(course(9L));
        when(courseClassMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        DimensionScore score = new DimensionScore();
        score.setQuestionId("project");
        score.setDimension("AWARENESS");
        score.setEarnedScore(BigDecimal.valueOf(9));
        score.setMaxScore(BigDecimal.TEN);
        when(dimensionScoreService.listBySources("project", List.of(31L))).thenReturn(List.of(score));

        assertThat(projectService.getSubmissionScoreDetails(31L))
                .singleElement()
                .satisfies(detail -> assertThat(detail.earnedScore()).isEqualByComparingTo("9"));

        ProjectSubmission another = submission(32L, 1L, 102L);
        when(submissionMapper.selectById(32L)).thenReturn(another);
        assertThatThrownBy(() -> projectService.getSubmissionScoreDetails(32L))
                .isInstanceOf(BizException.class)
                .hasMessage(ErrorCode.COURSE_ACCESS_DENIED.getMsg());
    }

    private static void setTeacher(Long id) {
        LoginUser loginUser = new LoginUser(id, "teacher" + id, "teacher", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    private static void setStudent(Long id, Long classId) {
        LoginUser loginUser = new LoginUser(id, "student" + id, "student", classId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    private static Project project(LocalDateTime deadline) {
        Project project = new Project();
        project.setId(1L);
        project.setSemesterId(3L);
        project.setName("项目作品");
        project.setDeadline(deadline);
        return project;
    }

    private static Semester semester() {
        Semester semester = new Semester();
        semester.setId(3L);
        semester.setCourseId(4L);
        return semester;
    }

    private static Course course(Long teacherId) {
        Course course = new Course();
        course.setId(4L);
        course.setTeacherId(teacherId);
        return course;
    }

    private static ProjectSubmission submission(Long id, Long projectId, Long studentId) {
        ProjectSubmission submission = new ProjectSubmission();
        submission.setId(id);
        submission.setProjectId(projectId);
        submission.setStudentId(studentId);
        submission.setContent("{}");
        submission.setStatus("submitted");
        return submission;
    }

    private static User student(Long id, String studentNo, String name) {
        User user = new User();
        user.setId(id);
        user.setRole("student");
        user.setStudentNo(studentNo);
        user.setName(name);
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

    private static CourseRoster roster(User... students) {
        return CourseRoster.of(List.of(students), List.of(schoolClass(10L)));
    }
}
