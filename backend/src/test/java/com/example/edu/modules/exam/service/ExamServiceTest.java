package com.example.edu.modules.exam.service;

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
import com.example.edu.modules.course.mapper.CourseMapper;
import com.example.edu.modules.course.mapper.CourseClassMapper;
import com.example.edu.modules.course.mapper.SemesterMapper;
import com.example.edu.modules.course.service.CourseRosterService;
import com.example.edu.modules.course.service.CourseRosterService.CourseRoster;
import com.example.edu.modules.evaluation.service.DimensionScoreService;
import com.example.edu.modules.evaluation.service.QuestionScoreHelper;
import com.example.edu.modules.exam.entity.Exam;
import com.example.edu.modules.exam.entity.ExamSubmission;
import com.example.edu.modules.exam.entity.ExamPaper;
import com.example.edu.modules.exam.mapper.ExamMapper;
import com.example.edu.modules.exam.mapper.ExamPaperMapper;
import com.example.edu.modules.exam.mapper.ExamSubmissionMapper;
import com.example.edu.modules.exam.vo.ExamSubmissionVO;
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
import static org.mockito.Mockito.doAnswer;
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
    @Mock private CourseClassMapper courseClassMapper;
    @Mock private CourseRosterService courseRosterService;
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
    void ownerTeacherCanListExamSubmissionInboxWithNotSubmittedStudents() {
        setTeacher(9L);
        examBelongsToTeacher(9L);
        when(courseRosterService.load(4L)).thenReturn(roster(
                student(102L, "20260002", "周二"),
                student(101L, "20260001", "林一")
        ));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(submission()));

        List<ExamSubmissionVO> inbox = examService.listSubmissionInbox(1L);

        assertThat(inbox).hasSize(2);
        ExamSubmissionVO submitted = inbox.stream().filter(row -> row.getStudentId().equals(101L)).findFirst().orElseThrow();
        assertThat(submitted.getId()).isEqualTo(31L);
        assertThat(submitted.getSubmissionId()).isEqualTo(31L);
        assertThat(submitted.getStatus()).isEqualTo("submitted");
        assertThat(submitted.getClassName()).isEqualTo("2026级1班");

        ExamSubmissionVO notSubmitted = inbox.stream().filter(row -> row.getStudentId().equals(102L)).findFirst().orElseThrow();
        assertThat(notSubmitted.getId()).isNull();
        assertThat(notSubmitted.getSubmissionId()).isNull();
        assertThat(notSubmitted.getStatus()).isEqualTo("not_submitted");
        assertThat(notSubmitted.getClassName()).isEqualTo("2026级1班");
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

    @Test
    void gradedExamCannotBeOverwrittenByStudent() {
        setStudent(101L, 10L);
        Exam exam = examInProgress();
        when(examMapper.selectById(1L)).thenReturn(exam);
        Semester semester = new Semester(); semester.setId(3L); semester.setCourseId(4L);
        Course course = new Course(); course.setId(4L); course.setTeacherId(9L);
        when(semesterMapper.selectById(3L)).thenReturn(semester);
        when(courseMapper.selectById(4L)).thenReturn(course);
        when(courseClassMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        ExamSubmission graded = submission(); graded.setStatus("graded"); graded.setScore(88);
        when(submissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(graded);

        assertThatThrownBy(() -> examService.submit(1L, "{\"q1\":\"changed\"}"))
                .isInstanceOf(BizException.class)
                .hasMessage(ErrorCode.SUBMISSION_LOCKED.getMsg());
        verify(submissionMapper, never()).updateById(graded);
    }

    @Test
    void ownerTeacherReturnsExamAndClearsScore() {
        setTeacher(9L);
        ExamSubmission graded = submission(); graded.setStatus("graded"); graded.setScore(88);
        when(submissionMapper.selectById(31L)).thenReturn(graded);
        examBelongsToTeacher(9L);

        examService.returnSubmission(31L, "请重新完成简答题");

        assertThat(graded.getStatus()).isEqualTo("returned");
        assertThat(graded.getScore()).isNull();
        assertThat(graded.getReturnReason()).isEqualTo("请重新完成简答题");
        verify(dimensionScoreService).clearScores("exam", 31L);
    }

    @Test
    void startingExamCreatesSubmissionAndAuditLog() {
        setStudent(101L, 10L);
        Exam exam = examInProgress();
        exam.setName("期中测试");
        when(examMapper.selectById(1L)).thenReturn(exam);
        Semester semester = new Semester(); semester.setId(3L); semester.setCourseId(4L);
        Course course = new Course(); course.setId(4L); course.setTeacherId(9L);
        when(semesterMapper.selectById(3L)).thenReturn(semester);
        when(courseMapper.selectById(4L)).thenReturn(course);
        when(courseClassMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(submissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            ExamSubmission inserted = invocation.getArgument(0);
            inserted.setId(41L);
            return 1;
        }).when(submissionMapper).insert(any(ExamSubmission.class));

        ExamSubmission started = examService.startExam(1L);

        assertThat(started.getStatus()).isEqualTo("in_progress");
        verify(auditLogService).record("开始考试", "exam_submission", 41L, "期中测试");
    }

    @Test
    void returnedExamResubmissionWritesAuditLog() {
        setStudent(101L, 10L);
        Exam exam = examInProgress();
        exam.setName("期中测试");
        when(examMapper.selectById(1L)).thenReturn(exam);
        Semester semester = new Semester(); semester.setId(3L); semester.setCourseId(4L);
        Course course = new Course(); course.setId(4L); course.setTeacherId(9L);
        when(semesterMapper.selectById(3L)).thenReturn(semester);
        when(courseMapper.selectById(4L)).thenReturn(course);
        when(courseClassMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        ExamSubmission returned = submission();
        returned.setStatus("returned");
        when(submissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(returned);

        examService.submit(1L, "{\"q1\":\"new answer\"}");

        verify(auditLogService).record("重新提交考试", "exam_submission", 31L, "期中测试");
    }

    @Test
    void studentPaperContentDoesNotExposeAnswers() {
        setStudent(101L, 10L);
        ExamPaper paper = new ExamPaper();
        paper.setId(7L);
        paper.setContent("{\"questions\":[{\"id\":\"q1\",\"title\":\"2+2?\",\"answer\":\"4\",\"autoGrade\":true,\"options\":[\"3\",\"4\"]}]}");

        String safeContent = examService.paperContentForCurrentUser(paper);

        assertThat(safeContent).contains("2+2?").contains("options").doesNotContain("answer")
                .doesNotContain("autoGrade");
    }

    @Test
    void teacherPaperContentRemainsCompleteForGrading() {
        setTeacher(9L);
        ExamPaper paper = new ExamPaper();
        paper.setId(7L);
        paper.setContent("{\"answer\":\"4\",\"autoGrade\":true}");

        assertThat(examService.paperContentForCurrentUser(paper))
                .contains("answer").contains("autoGrade");
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

    private static Exam examInProgress() {
        Exam exam = new Exam();
        exam.setId(1L);
        exam.setSemesterId(3L);
        exam.setStartTime(LocalDateTime.now().minusHours(1));
        exam.setEndTime(LocalDateTime.now().plusHours(1));
        return exam;
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
}
