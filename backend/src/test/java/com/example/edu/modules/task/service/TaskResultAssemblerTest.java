package com.example.edu.modules.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.Lesson;
import com.example.edu.modules.course.entity.Semester;
import com.example.edu.modules.course.mapper.CourseMapper;
import com.example.edu.modules.course.mapper.LessonMapper;
import com.example.edu.modules.course.mapper.SemesterMapper;
import com.example.edu.modules.evaluation.entity.DimensionScore;
import com.example.edu.modules.evaluation.entity.SubmissionFeedback;
import com.example.edu.modules.evaluation.mapper.SubmissionFeedbackMapper;
import com.example.edu.modules.evaluation.service.DimensionScoreService;
import com.example.edu.modules.task.entity.Submission;
import com.example.edu.modules.task.entity.Task;
import com.example.edu.modules.task.mapper.SubmissionMapper;
import com.example.edu.modules.task.vo.TaskResultVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskResultAssemblerTest {

    @Mock private SubmissionMapper submissionMapper;
    @Mock private LessonMapper lessonMapper;
    @Mock private SemesterMapper semesterMapper;
    @Mock private CourseMapper courseMapper;
    @Mock private SubmissionFeedbackMapper submissionFeedbackMapper;
    @Mock private DimensionScoreService dimensionScoreService;

    @InjectMocks
    private TaskResultAssembler assembler;

    @Test
    void buildReturnsGradedQuestionFeedbackAndDimensionSummary() {
        Task task = task();
        Submission submission = submission(11L, 101L, "{\"q1\":\"A\",\"q2\":[\"B\",\"C\"]}", "graded");
        SubmissionFeedback feedback = new SubmissionFeedback();
        feedback.setSubmissionId(11L);
        feedback.setTeacherComment("整体很好");
        feedback.setGradedAt(LocalDateTime.of(2026, 6, 13, 19, 30));
        feedback.setQuestionFeedback("""
                [{"questionId":"q2","comment":"多选题表达准确","referenceAnswerVisible":true}]
                """);

        when(submissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(submission);
        when(lessonMapper.selectById(2L)).thenReturn(lesson());
        when(semesterMapper.selectById(3L)).thenReturn(semester());
        Course course = course();
        course.setName("Python 入门");
        when(courseMapper.selectById(4L)).thenReturn(course);
        when(submissionFeedbackMapper.selectById(11L)).thenReturn(feedback);
        when(dimensionScoreService.listBySources("process", List.of(11L))).thenReturn(List.of(
                score("q1", "COMPUTING", "4", "4", true),
                score("q2", "AWARENESS", "3", "4", false)
        ));

        TaskResultVO result = assembler.build(task, 101L);

        assertThat(result.getStatus()).isEqualTo("graded");
        assertThat(result.getTask().getCourseName()).isEqualTo("Python 入门");
        assertThat(result.getSubmission().getTeacherComment()).isEqualTo("整体很好");
        assertThat(result.getQuestions()).hasSize(2);
        assertThat(result.getQuestionResults()).hasSize(2);
        assertThat(result.getQuestionResults().get(0).getCorrect()).isTrue();
        assertThat(result.getQuestionResults().get(1).getComment()).isEqualTo("多选题表达准确");
        assertThat(result.getDimensionSummary()).extracting(TaskResultVO.DimensionSummary::getDimension)
                .containsExactly("AWARENESS", "COMPUTING");
    }

    @Test
    void buildReturnsNotSubmittedWhenStudentHasNoSubmission() {
        Task task = task();
        when(submissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(lessonMapper.selectById(2L)).thenReturn(lesson());
        when(semesterMapper.selectById(3L)).thenReturn(semester());
        when(courseMapper.selectById(4L)).thenReturn(course());

        TaskResultVO result = assembler.build(task, 101L);

        assertThat(result.getStatus()).isEqualTo("not_submitted");
        assertThat(result.getSubmission()).isNull();
        assertThat(result.getQuestionResults()).isEmpty();
        assertThat(result.getQuestions()).hasSize(2);
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

    private static DimensionScore score(String questionId, String dimension, String earned, String max, boolean autoGraded) {
        DimensionScore score = new DimensionScore();
        score.setQuestionId(questionId);
        score.setDimension(dimension);
        score.setEarnedScore(new BigDecimal(earned));
        score.setMaxScore(new BigDecimal(max));
        score.setAutoGraded(autoGraded);
        return score;
    }
}
