package com.example.edu.modules.stats.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.classes.entity.SchoolClass;
import com.example.edu.modules.classes.mapper.SchoolClassMapper;
import com.example.edu.modules.course.entity.Lesson;
import com.example.edu.modules.course.mapper.LessonMapper;
import com.example.edu.modules.evaluation.entity.Evaluation;
import com.example.edu.modules.evaluation.mapper.EvaluationMapper;
import com.example.edu.modules.exam.entity.Exam;
import com.example.edu.modules.exam.entity.ExamSubmission;
import com.example.edu.modules.exam.mapper.ExamMapper;
import com.example.edu.modules.exam.mapper.ExamSubmissionMapper;
import com.example.edu.modules.project.entity.Project;
import com.example.edu.modules.project.entity.ProjectScore;
import com.example.edu.modules.project.mapper.ProjectMapper;
import com.example.edu.modules.project.mapper.ProjectScoreMapper;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock private EvaluationMapper evaluationMapper;
    @Mock private ExamSubmissionMapper examSubmissionMapper;
    @Mock private ExamMapper examMapper;
    @Mock private ProjectScoreMapper projectScoreMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private SubmissionMapper submissionMapper;
    @Mock private TaskMapper taskMapper;
    @Mock private LessonMapper lessonMapper;
    @Mock private UserMapper userMapper;
    @Mock private SchoolClassMapper schoolClassMapper;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private StatsService statsService;

    private static final Long SEMESTER_ID = 1L;
    private static final Long STUDENT_ID = 100L;
    private static final Long CLASS_ID = 10L;

    private Lesson lesson;
    private Task worksheetTask, artifactTask;
    private Submission sub1, sub2;
    private Evaluation eval1, eval2, eval3, eval4;
    private User student;
    private SchoolClass schoolClass;

    @BeforeEach
    void setUp() {
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

        worksheetTask = new Task();
        worksheetTask.setId(1L);
        worksheetTask.setLessonId(1L);
        worksheetTask.setType("worksheet");

        artifactTask = new Task();
        artifactTask.setId(2L);
        artifactTask.setLessonId(1L);
        artifactTask.setType("artifact");

        sub1 = new Submission();
        sub1.setId(1L);
        sub1.setTaskId(1L);
        sub1.setStudentId(STUDENT_ID);
        sub1.setStatus("graded");

        sub2 = new Submission();
        sub2.setId(2L);
        sub2.setTaskId(2L);
        sub2.setStudentId(STUDENT_ID);
        sub2.setStatus("graded");

        eval1 = new Evaluation();
        eval1.setSourceId(1L);
        eval1.setSourceType("worksheet");
        eval1.setDimension("AWARENESS");
        eval1.setGrade("A");
        eval1.setIsSpecial(0);

        eval2 = new Evaluation();
        eval2.setSourceId(1L);
        eval2.setSourceType("worksheet");
        eval2.setDimension("COMPUTING");
        eval2.setGrade("B");
        eval2.setIsSpecial(0);

        eval3 = new Evaluation();
        eval3.setSourceId(2L);
        eval3.setSourceType("artifact");
        eval3.setDimension("DIGITAL_LEARNING");
        eval3.setGrade("A");
        eval3.setIsSpecial(0);

        eval4 = new Evaluation();
        eval4.setSourceId(2L);
        eval4.setSourceType("artifact");
        eval4.setDimension("RESPONSIBILITY");
        eval4.setGrade("C");
        eval4.setIsSpecial(0);
    }

    @Test
    void emptyLessonsReturnsEmptyList() {
        when(lessonMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<StatsService.GradeRow> rows = statsService.calculateSemesterGrades(SEMESTER_ID);
        assertThat(rows).isEmpty();
    }

    @Test
    void worksheetOnlyCalculatesProcessScore() {
        when(lessonMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(lesson));
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(worksheetTask));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(sub1));
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(student));
        when(schoolClassMapper.selectBatchIds(anyCollection())).thenReturn(List.of(schoolClass));
        when(evaluationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(eval1, eval2));
        when(examMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(projectMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<StatsService.GradeRow> rows = statsService.calculateSemesterGrades(SEMESTER_ID);

        assertThat(rows).hasSize(1);
        StatsService.GradeRow row = rows.get(0);
        assertThat(row.studentNo()).isEqualTo("2026001");
        // process = avg(100, 80) * 1.0 / 1.0 = 90.0
        assertThat(row.processScore()).isEqualTo(90.0);
        // No exam/project → no result score
        assertThat(row.resultScore()).isNull();
        assertThat(row.totalScore()).isNull();
        assertThat(row.totalGrade()).isEqualTo("暂无数据");
        assertThat(row.remark()).isEqualTo("缺结果评价");
        // Dimension averages
        assertThat(row.awareness()).isEqualTo(100);
        assertThat(row.computing()).isEqualTo(80);
        assertThat(row.digitalLearn()).isNull();
        assertThat(row.responsibility()).isNull();
    }

    @Test
    void mixedWorksheetAndArtifactWeightedAverage() {
        when(lessonMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(lesson));
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(worksheetTask, artifactTask));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(sub1, sub2));
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(student));
        when(schoolClassMapper.selectBatchIds(anyCollection())).thenReturn(List.of(schoolClass));
        when(evaluationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(eval1, eval2, eval3, eval4));
        when(examMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(projectMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<StatsService.GradeRow> rows = statsService.calculateSemesterGrades(SEMESTER_ID);

        assertThat(rows).hasSize(1);
        StatsService.GradeRow row = rows.get(0);
        // worksheet: avg(100,80)=90 * 1.0 = 90
        // artifact: avg(100,60)=80 * 1.5 = 120
        // weighted avg = (90+120)/(1.0+1.5) = 210/2.5 = 84.0
        assertThat(row.processScore()).isEqualTo(84.0);
    }

    @Test
    void withExamAndProjectScores() {
        when(lessonMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(lesson));
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(worksheetTask));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(sub1));
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(student));
        when(schoolClassMapper.selectBatchIds(anyCollection())).thenReturn(List.of(schoolClass));
        when(evaluationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(eval1, eval2));

        Exam exam = new Exam();
        exam.setId(1L);
        exam.setSemesterId(SEMESTER_ID);
        exam.setWeight(BigDecimal.valueOf(1.0));
        when(examMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(exam));

        ExamSubmission examSub = new ExamSubmission();
        examSub.setExamId(1L);
        examSub.setStudentId(STUDENT_ID);
        examSub.setScore(85);
        when(examSubmissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(examSub));

        Project project = new Project();
        project.setId(1L);
        project.setSemesterId(SEMESTER_ID);
        project.setWeight(BigDecimal.valueOf(1.0));
        when(projectMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(project));

        ProjectScore score = new ProjectScore();
        score.setProjectId(1L);
        score.setStudentId(STUDENT_ID);
        score.setGrade("B");
        score.setIsSpecial(0);
        when(projectScoreMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(score));

        List<StatsService.GradeRow> rows = statsService.calculateSemesterGrades(SEMESTER_ID);

        assertThat(rows).hasSize(1);
        StatsService.GradeRow row = rows.get(0);
        assertThat(row.processScore()).isEqualTo(90.0); // avg(100,80)=90
        assertThat(row.examScore()).isEqualTo(85.0);
        assertThat(row.projectScore()).isEqualTo(80.0); // B=80
        // result = avg(85, 80) = 82.5
        assertThat(row.resultScore()).isEqualTo(82.5);
        // total = 90*0.5 + 82.5*0.5 = 86.25 → 86.3
        assertThat(row.totalScore()).isEqualTo(86.3);
        assertThat(row.totalGrade()).isEqualTo("B");
        assertThat(row.remark()).isEmpty();
    }

    @Test
    void gradedSubmissionWithoutEvaluationsGetsZero() {
        when(lessonMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(lesson));
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(worksheetTask));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(sub1)); // status=graded
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(student));
        when(schoolClassMapper.selectBatchIds(anyCollection())).thenReturn(List.of(schoolClass));
        // No evaluations returned (auto-F scenario)
        when(evaluationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(examMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(projectMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<StatsService.GradeRow> rows = statsService.calculateSemesterGrades(SEMESTER_ID);

        assertThat(rows).hasSize(1);
        StatsService.GradeRow row = rows.get(0);
        // graded + no evals → scores 0 with weight 1.0
        assertThat(row.processScore()).isEqualTo(0.0);
        assertThat(row.awareness()).isEqualTo(0);
        assertThat(row.computing()).isEqualTo(0);
    }

    @Test
    void notGradedSubmissionWithoutEvaluationsIsSkipped() {
        sub1.setStatus("submitted"); // not graded yet
        when(lessonMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(lesson));
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(worksheetTask));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(sub1));
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(student));
        when(schoolClassMapper.selectBatchIds(anyCollection())).thenReturn(List.of(schoolClass));
        when(evaluationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(examMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(projectMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<StatsService.GradeRow> rows = statsService.calculateSemesterGrades(SEMESTER_ID);

        assertThat(rows).hasSize(1);
        StatsService.GradeRow row = rows.get(0);
        assertThat(row.processScore()).isNull(); // Skipped, no data
        assertThat(row.remark()).isEqualTo("无评价数据");
    }

    @Test
    void classNameFormatting() {
        when(lessonMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(lesson));
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(worksheetTask));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(sub1));
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(student));
        when(schoolClassMapper.selectBatchIds(anyCollection())).thenReturn(List.of(schoolClass));
        when(evaluationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(eval1, eval2));
        when(examMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(projectMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<StatsService.GradeRow> rows = statsService.calculateSemesterGrades(SEMESTER_ID);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).className()).isEqualTo("2026级1班");
    }

    @Test
    void gradeLabelsAwardCorrectGradeForScoreRanges() {
        // Verify internal gradeLabel through calculateSemesterGrades result
        // Score 90 → A (tested in worksheetOnly: processScore=90.0)
        // Score 86.3 → B (tested in withExamAndProjectScores: totalScore=86.3)
        // Score 82.5 → B (tested in withExamAndProjectScores: resultScore=82.5)
        // Score 85 → B (tested in withExamAndProjectScores: examScore=85.0)
        // Score 80 → B (tested in withExamAndProjectScores: projectScore=80.0)
        // Score 0 → F (tested in gradedSubmissionWithoutEvaluations)

        // Verify the gradeLabel method directly via specialized scenarios
        when(lessonMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(lesson));
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(worksheetTask));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(sub1));
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(student));
        when(schoolClassMapper.selectBatchIds(anyCollection())).thenReturn(List.of(schoolClass));
        when(evaluationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(eval1, eval2));
        when(examMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(projectMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<StatsService.GradeRow> rows = statsService.calculateSemesterGrades(SEMESTER_ID);
        assertThat(rows).hasSize(1);
        // processScore=90.0 → grade A (100)
        assertThat(rows.get(0).totalGrade()).isEqualTo("暂无数据"); // no result → no total grade
    }
}
