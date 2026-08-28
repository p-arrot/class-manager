package com.example.edu.modules.evaluation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.CourseClass;
import com.example.edu.modules.course.entity.Lesson;
import com.example.edu.modules.course.entity.Semester;
import com.example.edu.modules.course.mapper.*;
import com.example.edu.modules.evaluation.dto.EvaluateDTO;
import com.example.edu.modules.evaluation.entity.Evaluation;
import com.example.edu.modules.evaluation.mapper.EvaluationMapper;
import com.example.edu.modules.evaluation.mapper.SubmissionFeedbackMapper;
import com.example.edu.modules.evaluation.service.DimensionScoreService;
import com.example.edu.modules.evaluation.service.impl.EvaluationServiceImpl;
import com.example.edu.modules.task.entity.Submission;
import com.example.edu.modules.task.entity.Task;
import com.example.edu.modules.task.mapper.SubmissionMapper;
import com.example.edu.modules.task.mapper.TaskMapper;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceImplTest {

    @Mock private EvaluationMapper evaluationMapper;
    @Mock private SubmissionFeedbackMapper submissionFeedbackMapper;
    @Mock private DimensionScoreService dimensionScoreService;
    @Mock private SubmissionMapper submissionMapper;
    @Mock private TaskMapper taskMapper;
    @Mock private LessonMapper lessonMapper;
    @Mock private SemesterMapper semesterMapper;
    @Mock private CourseMapper courseMapper;
    @Mock private CourseClassMapper courseClassMapper;
    @Mock private UserMapper userMapper;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private EvaluationServiceImpl evaluationService;

    private MockedStatic<SecurityUtils> securityUtilsMock;
    private Submission submission;
    private Task task;
    private Lesson lesson;
    private Semester semester;
    private Course course;

    @BeforeEach
    void setUp() {
        // Mock static SecurityUtils — default to teacher role
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(200L);
        securityUtilsMock.when(SecurityUtils::getCurrentUserRole).thenReturn("teacher");

        submission = new Submission();
        submission.setId(1L);
        submission.setTaskId(10L);
        submission.setStudentId(100L);
        submission.setStatus("submitted");

        task = new Task();
        task.setId(10L);
        task.setLessonId(20L);
        task.setType("worksheet");
        task.setDeadline(LocalDateTime.now().plusDays(7));

        lesson = new Lesson();
        lesson.setId(20L);
        lesson.setSemesterId(30L);

        semester = new Semester();
        semester.setId(30L);
        semester.setCourseId(40L);
        semester.setStartTime(LocalDateTime.of(2026, 3, 1, 0, 0));
        semester.setEndTime(LocalDateTime.of(2026, 7, 1, 0, 0));

        course = new Course();
        course.setId(40L);
        course.setTeacherId(200L);

        User rosterStudent = new User();
        rosterStudent.setId(100L);
        rosterStudent.setRole("student");
        rosterStudent.setClassId(10L);
        lenient().when(semesterMapper.selectById(anyLong())).thenReturn(semester);
        lenient().when(courseMapper.selectById(anyLong())).thenReturn(course);
        lenient().when(courseClassMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        lenient().when(userMapper.selectById(anyLong())).thenReturn(rosterStudent);
    }

    @Test
    void returningTaskSubmissionClearsAssessmentAndStoresReason() {
        when(submissionMapper.selectById(1L)).thenReturn(submission);
        when(taskMapper.selectById(10L)).thenReturn(task);
        when(lessonMapper.selectById(20L)).thenReturn(lesson);

        evaluationService.returnSubmission(1L, "请补充算法说明");

        assertThat(submission.getStatus()).isEqualTo("returned");
        assertThat(submission.getReturnReason()).isEqualTo("请补充算法说明");
        assertThat(submission.getReturnedAt()).isNotNull();
        verify(dimensionScoreService).clearScores("process", 1L);
        verify(submissionFeedbackMapper).deleteById(1L);
        verify(submissionMapper).updateById(submission);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    // ── evaluate ──────────────────────────────────────────────────

    @Test
    void evaluateWithDimensionsMarksGraded() {
        when(submissionMapper.selectById(1L)).thenReturn(submission);
        when(taskMapper.selectById(10L)).thenReturn(task);
        when(lessonMapper.selectById(20L)).thenReturn(lesson);
        when(semesterMapper.selectById(30L)).thenReturn(semester);
        when(courseMapper.selectById(40L)).thenReturn(course);
        when(evaluationMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(evaluationMapper.insert(Mockito.<Evaluation>any())).thenReturn(1);

        EvaluateDTO dto = new EvaluateDTO();
        EvaluateDTO.DimensionGrade dg1 = new EvaluateDTO.DimensionGrade();
        dg1.setDimension("AWARENESS");
        dg1.setGrade("A");
        EvaluateDTO.DimensionGrade dg2 = new EvaluateDTO.DimensionGrade();
        dg2.setDimension("COMPUTING");
        dg2.setGrade("B");
        dto.setDimensions(List.of(dg1, dg2));

        evaluationService.evaluate(1L, dto);

        verify(submissionMapper).updateById(Mockito.<Submission>argThat(s -> "graded".equals(s.getStatus())));
        verify(evaluationMapper, times(2)).insert(Mockito.<Evaluation>any());
    }

    @Test
    void evaluateWithSpecialMarksSpecialStatus() {
        when(submissionMapper.selectById(1L)).thenReturn(submission);
        when(taskMapper.selectById(10L)).thenReturn(task);
        when(lessonMapper.selectById(20L)).thenReturn(lesson);
        when(semesterMapper.selectById(30L)).thenReturn(semester);
        when(courseMapper.selectById(40L)).thenReturn(course);
        when(evaluationMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);
        when(evaluationMapper.insert(Mockito.<Evaluation>any())).thenReturn(1);

        EvaluateDTO dto = new EvaluateDTO();
        dto.setIsSpecial(true);

        evaluationService.evaluate(1L, dto);

        verify(submissionMapper).updateById(Mockito.<Submission>argThat(s -> "special".equals(s.getStatus())));
        verify(evaluationMapper, times(1)).insert(Mockito.<Evaluation>any());
    }

    @Test
    void evaluateWithoutDimensionsResetsToSubmitted() {
        submission.setStatus("graded");
        when(submissionMapper.selectById(1L)).thenReturn(submission);
        when(taskMapper.selectById(10L)).thenReturn(task);
        when(lessonMapper.selectById(20L)).thenReturn(lesson);
        when(semesterMapper.selectById(30L)).thenReturn(semester);
        when(courseMapper.selectById(40L)).thenReturn(course);
        when(evaluationMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        EvaluateDTO dto = new EvaluateDTO();

        evaluationService.evaluate(1L, dto);

        verify(submissionMapper).updateById(Mockito.<Submission>argThat(s -> "submitted".equals(s.getStatus())));
        verify(evaluationMapper, never()).insert(Mockito.<Evaluation>any());
    }

    @Test
    void evaluateMissingSubmissionThrows() {
        when(submissionMapper.selectById(999L)).thenReturn(null);

        EvaluateDTO dto = new EvaluateDTO();
        assertThatThrownBy(() -> evaluationService.evaluate(999L, dto))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.SUBMISSION_NOT_FOUND.getCode());
    }

    // ── getGradeScores ────────────────────────────────────────────

    @Test
    void gradeScoresReturnsCorrectMapping() {
        var scores = evaluationService.getGradeScores();
        assertThat(scores).containsEntry("A", 100);
        assertThat(scores).containsEntry("E", 20);
        assertThat(scores).containsEntry("F", 0);
    }

    // ── getStudentEvaluations ─────────────────────────────────────

    @Test
    void getStudentEvaluationsEmptyWhenNoTasks() {
        when(lessonMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        var evals = evaluationService.getStudentEvaluations(100L, 1L);
        assertThat(evals).isEmpty();
    }

    @Test
    void studentCannotReadAnotherStudentsEvaluations() {
        securityUtilsMock.when(SecurityUtils::getCurrentUserRole).thenReturn("student");
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(100L);

        assertThatThrownBy(() -> evaluationService.getStudentEvaluations(101L, 1L))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.COURSE_ACCESS_DENIED.getCode());
    }

    @Test
    void getStudentEvaluationsReturnsMappedVOs() {
        Lesson l = new Lesson();
        l.setId(1L);
        l.setSemesterId(1L);
        when(lessonMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(l));
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(submission));

        Evaluation e1 = new Evaluation();
        e1.setSourceType("worksheet");
        e1.setSourceId(1L);
        e1.setDimension("AWARENESS");
        e1.setGrade("A");
        e1.setIsSpecial(0);
        when(evaluationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(e1));

        var evals = evaluationService.getStudentEvaluations(100L, 1L);
        assertThat(evals).hasSize(1);
        assertThat(evals.get(0).getScore()).isEqualTo(100);
        assertThat(evals.get(0).getLabel()).isEqualTo("信息意识");
        assertThat(evals.get(0).getTaskId()).isEqualTo(10L);
        assertThat(evals.get(0).getSubmissionId()).isEqualTo(1L);
        assertThat(evals.get(0).getTaskStatus()).isEqualTo("submitted");
    }

    @Test
    void getStudentEvaluationsEmptyWhenNoSemesterSubmissions() {
        Lesson l = new Lesson();
        l.setId(1L);
        l.setSemesterId(1L);
        when(lessonMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(l));
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        var evals = evaluationService.getStudentEvaluations(100L, 1L);

        assertThat(evals).isEmpty();
        verify(evaluationMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    // ── getRadar ──────────────────────────────────────────────────

    @Test
    void radarReturnsAllFourDimensions() {
        Lesson l = new Lesson();
        l.setId(1L);
        l.setSemesterId(1L);
        when(lessonMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(l));
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(submission));

        Evaluation e1 = new Evaluation();
        e1.setSourceType("worksheet");
        e1.setSourceId(1L);
        e1.setDimension("AWARENESS");
        e1.setGrade("A");
        e1.setIsSpecial(0);
        when(evaluationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(e1));

        when(semesterMapper.selectById(1L)).thenReturn(semester);
        when(semesterMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        var radar = evaluationService.getRadar(100L, 1L);

        assertThat(radar.getCurrent()).hasSize(4);
        var awarenessScore = radar.getCurrent().stream()
                .filter(d -> "AWARENESS".equals(d.getDimension()))
                .findFirst().orElseThrow();
        assertThat(awarenessScore.getAvgScore()).isEqualTo(100.0);

        var computingScore = radar.getCurrent().stream()
                .filter(d -> "COMPUTING".equals(d.getDimension()))
                .findFirst().orElseThrow();
        assertThat(computingScore.getAvgScore()).isEqualTo(0.0);
    }

    @Test
    void radarWithPreviousSemesterIncludesComparison() {
        Lesson l = new Lesson();
        l.setId(1L);
        l.setSemesterId(1L);
        when(lessonMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(l));
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(submission));

        Evaluation e1 = new Evaluation();
        e1.setSourceType("worksheet");
        e1.setSourceId(1L);
        e1.setDimension("COMPUTING");
        e1.setGrade("B");
        e1.setIsSpecial(0);
        when(evaluationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(e1));

        when(semesterMapper.selectById(1L)).thenReturn(semester);
        Semester prevSemester = new Semester();
        prevSemester.setId(0L);
        prevSemester.setCourseId(40L);
        prevSemester.setStartTime(LocalDateTime.of(2025, 9, 1, 0, 0));
        when(semesterMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(prevSemester));

        var radar = evaluationService.getRadar(100L, 1L);

        assertThat(radar.getCurrent()).hasSize(4);
        assertThat(radar.getPrevious()).hasSize(4);
        assertThat(radar.isHasPrevious()).isTrue();
    }

    // ── autoGradeMissedDeadlines ──────────────────────────────────

    @Test
    void autoGradeCreatesFForMissingStudents() {
        task.setDeadline(LocalDateTime.now().minusDays(1));
        when(taskMapper.selectById(10L)).thenReturn(task);
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(lessonMapper.selectById(20L)).thenReturn(lesson);
        when(semesterMapper.selectById(30L)).thenReturn(semester);
        when(courseMapper.selectById(40L)).thenReturn(course);
        when(courseClassMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(new CourseClass() {{ setClassId(10L); }}));
        when(userMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(createStudent(100L), createStudent(101L)));
        when(submissionMapper.insert(Mockito.<Submission>any())).thenReturn(1);
        when(evaluationMapper.insert(Mockito.<Evaluation>any())).thenReturn(1);

        evaluationService.autoGradeMissedDeadlines(10L);

        verify(submissionMapper, times(2)).insert(Mockito.<Submission>any());
        verify(evaluationMapper, times(8)).insert(Mockito.<Evaluation>any());

        ArgumentCaptor<Submission> subCaptor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionMapper, times(2)).insert(subCaptor.capture());
        subCaptor.getAllValues().forEach(s -> {
            assertThat(s.getStatus()).isEqualTo("graded");
            assertThat(s.getContent()).isEqualTo("{\"auto\":\"F\"}");
        });
    }

    @Test
    void autoGradeSkipsWhenNoDeadline() {
        task.setDeadline(null);
        when(taskMapper.selectById(10L)).thenReturn(task);

        evaluationService.autoGradeMissedDeadlines(10L);

        verify(submissionMapper, never()).insert(Mockito.<Submission>any());
    }

    @Test
    void autoGradeSkipsAlreadySubmittedStudents() {
        task.setDeadline(LocalDateTime.now().minusDays(1));
        when(taskMapper.selectById(10L)).thenReturn(task);

        Submission existingSub = new Submission();
        existingSub.setTaskId(10L);
        existingSub.setStudentId(100L);
        existingSub.setStatus("submitted");
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(existingSub));

        when(lessonMapper.selectById(20L)).thenReturn(lesson);
        when(semesterMapper.selectById(30L)).thenReturn(semester);
        when(courseMapper.selectById(40L)).thenReturn(course);
        when(courseClassMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(new CourseClass() {{ setClassId(10L); }}));
        when(userMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(createStudent(100L), createStudent(101L)));
        when(submissionMapper.insert(Mockito.<Submission>any())).thenReturn(1);
        when(evaluationMapper.insert(Mockito.<Evaluation>any())).thenReturn(1);

        evaluationService.autoGradeMissedDeadlines(10L);

        verify(submissionMapper, times(1)).insert(Mockito.<Submission>any());
    }

    // ── helpers ───────────────────────────────────────────────────

    private User createStudent(Long id) {
        User u = new User();
        u.setId(id);
        u.setRole("student");
        u.setName("学生" + id);
        u.setClassId(10L);
        return u;
    }
}
