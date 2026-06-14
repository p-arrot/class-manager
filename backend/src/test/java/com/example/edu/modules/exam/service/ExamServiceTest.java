package com.example.edu.modules.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.LoginUser;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.Semester;
import com.example.edu.modules.course.mapper.CourseMapper;
import com.example.edu.modules.course.mapper.SemesterMapper;
import com.example.edu.modules.evaluation.service.DimensionScoreService;
import com.example.edu.modules.evaluation.service.QuestionScoreHelper;
import com.example.edu.modules.exam.entity.Exam;
import com.example.edu.modules.exam.entity.ExamSubmission;
import com.example.edu.modules.exam.mapper.ExamMapper;
import com.example.edu.modules.exam.mapper.ExamPaperMapper;
import com.example.edu.modules.exam.mapper.ExamSubmissionMapper;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock private ExamMapper examMapper;
    @Mock private ExamPaperMapper paperMapper;
    @Mock private ExamSubmissionMapper submissionMapper;
    @Mock private SemesterMapper semesterMapper;
    @Mock private CourseMapper courseMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private DimensionScoreService dimensionScoreService;
    @Mock private QuestionScoreHelper questionScoreHelper;

    @InjectMocks
    private ExamService examService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nonOwnerTeacherCannotListExamSubmissions() {
        setTeacher(8L);
        examBelongsToTeacher(9L);

        assertThatThrownBy(() -> examService.listSubmissions(1L))
                .isInstanceOf(BizException.class)
                .hasMessage(ErrorCode.COURSE_ACCESS_DENIED.getMsg());
        verify(submissionMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void ownerTeacherCanListExamSubmissions() {
        setTeacher(9L);
        examBelongsToTeacher(9L);
        ExamSubmission submission = submission();
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(submission));

        List<ExamSubmission> submissions = examService.listSubmissions(1L);

        assertThat(submissions).containsExactly(submission);
    }

    @Test
    void nonOwnerTeacherCannotGradeExamSubmission() {
        setTeacher(8L);
        when(submissionMapper.selectById(31L)).thenReturn(submission());
        examBelongsToTeacher(9L);

        assertThatThrownBy(() -> examService.gradeSubmission(31L, 80, false, List.of()))
                .isInstanceOf(BizException.class)
                .hasMessage(ErrorCode.COURSE_ACCESS_DENIED.getMsg());
        verify(submissionMapper, never()).updateById(any(ExamSubmission.class));
        verify(dimensionScoreService, never()).replaceScores(any(), any(), any(), any());
    }

    @Test
    void ownerTeacherGradesExamSubmissionWithDimensionScores() {
        setTeacher(9L);
        ExamSubmission submission = submission();
        when(submissionMapper.selectById(31L)).thenReturn(submission);
        examBelongsToTeacher(9L);
        List<DimensionScoreService.ScoreInput> scores = List.of(
                new DimensionScoreService.ScoreInput("q1", "COMPUTING", BigDecimal.valueOf(8), BigDecimal.TEN, false)
        );

        examService.gradeSubmission(31L, 80, false, scores);

        ArgumentCaptor<ExamSubmission> captor = ArgumentCaptor.forClass(ExamSubmission.class);
        verify(submissionMapper).updateById(captor.capture());
        assertThat(captor.getValue().getScore()).isEqualTo(80);
        assertThat(captor.getValue().getStatus()).isEqualTo("graded");
        verify(dimensionScoreService).replaceScores("exam", 31L, 101L, scores);
    }

    @Test
    void ownerTeacherMarksExamSubmissionAbsent() {
        setTeacher(9L);
        when(submissionMapper.selectById(31L)).thenReturn(submission());
        examBelongsToTeacher(9L);

        examService.gradeSubmission(31L, null, true, List.of(
                new DimensionScoreService.ScoreInput("q1", "COMPUTING", BigDecimal.valueOf(8), BigDecimal.TEN, false)
        ));

        ArgumentCaptor<ExamSubmission> captor = ArgumentCaptor.forClass(ExamSubmission.class);
        verify(submissionMapper).updateById(captor.capture());
        assertThat(captor.getValue().getScore()).isZero();
        assertThat(captor.getValue().getStatus()).isEqualTo("absent");
        verify(dimensionScoreService).replaceScores("exam", 31L, 101L, List.of());
    }

    private void examBelongsToTeacher(Long teacherId) {
        Exam exam = new Exam();
        exam.setId(1L);
        exam.setSemesterId(3L);
        when(examMapper.selectById(1L)).thenReturn(exam);

        Semester semester = new Semester();
        semester.setId(3L);
        semester.setCourseId(4L);
        when(semesterMapper.selectById(3L)).thenReturn(semester);

        Course course = new Course();
        course.setId(4L);
        course.setTeacherId(teacherId);
        when(courseMapper.selectById(4L)).thenReturn(course);
    }

    private static ExamSubmission submission() {
        ExamSubmission submission = new ExamSubmission();
        submission.setId(31L);
        submission.setExamId(1L);
        submission.setStudentId(101L);
        submission.setAnswers("{}");
        submission.setStatus("submitted");
        return submission;
    }

    private static void setTeacher(Long id) {
        LoginUser loginUser = new LoginUser(id, "teacher" + id, "teacher", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }
}
