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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskResultAssembler {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final SubmissionMapper submissionMapper;
    private final LessonMapper lessonMapper;
    private final SemesterMapper semesterMapper;
    private final CourseMapper courseMapper;
    private final SubmissionFeedbackMapper submissionFeedbackMapper;
    private final DimensionScoreService dimensionScoreService;

    public TaskResultVO build(Task task, Long studentId) {
        List<Map<String, Object>> questions = "worksheet".equals(task.getType())
                ? parseQuestions(task.getFormSchema())
                : List.of();
        Submission sub = submissionMapper.selectOne(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getTaskId, task.getId())
                .eq(Submission::getStudentId, studentId));
        if (sub == null) {
            return TaskResultVO.builder()
                    .task(buildTaskInfo(task))
                    .status("not_submitted")
                    .submission(null)
                    .questions(buildQuestionInfo(questions, Map.of()))
                    .answers(Map.of())
                    .questionResults(List.of())
                    .dimensionSummary(List.of())
                    .build();
        }

        SubmissionFeedback feedback = submissionFeedbackMapper.selectById(sub.getId());
        Map<String, QuestionFeedbackItem> feedbackMap = parseQuestionFeedback(feedback);
        List<DimensionScore> scores = dimensionScoreService.listBySources("process", List.of(sub.getId()));
        Map<String, List<DimensionScore>> scoresByQuestion = scores.stream()
                .collect(Collectors.groupingBy(score -> score.getQuestionId() == null ? "" : score.getQuestionId()));
        Map<String, Object> answers = parseAnswerMap(sub.getContent());
        boolean showResults = "graded".equals(sub.getStatus()) || "special".equals(sub.getStatus());

        return TaskResultVO.builder()
                .task(buildTaskInfo(task))
                .status(sub.getStatus())
                .submission(TaskResultVO.SubmissionResult.builder()
                        .id(sub.getId())
                        .status(sub.getStatus())
                        .content(sub.getContent())
                        .submittedAt(sub.getSubmittedAt())
                        .gradedAt(feedback != null ? feedback.getGradedAt() : null)
                        .teacherComment(feedback != null ? feedback.getTeacherComment() : null)
                        .build())
                .questions(buildQuestionInfo(questions, feedbackMap))
                .answers(answers)
                .questionResults(showResults ? buildQuestionResults(questions, answers, scoresByQuestion, feedbackMap) : List.of())
                .dimensionSummary(showResults ? buildDimensionSummary(scores) : List.of())
                .build();
    }

    private TaskResultVO.TaskInfo buildTaskInfo(Task task) {
        Lesson lesson = lessonMapper.selectById(task.getLessonId());
        Semester semester = lesson != null ? semesterMapper.selectById(lesson.getSemesterId()) : null;
        Course course = semester != null ? courseMapper.selectById(semester.getCourseId()) : null;
        return TaskResultVO.TaskInfo.builder()
                .id(task.getId())
                .title(task.getTitle())
                .type(task.getType())
                .courseId(course != null ? course.getId() : null)
                .courseName(course != null ? course.getName() : null)
                .lessonId(lesson != null ? lesson.getId() : null)
                .lessonName(lesson != null ? lesson.getName() : null)
                .build();
    }

    private List<TaskResultVO.QuestionInfo> buildQuestionInfo(
            List<Map<String, Object>> questions,
            Map<String, QuestionFeedbackItem> feedbackMap) {
        List<TaskResultVO.QuestionInfo> result = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> question = questions.get(i);
            String questionId = questionId(question);
            boolean autoGradable = isAutoGradable(question);
            QuestionFeedbackItem feedback = feedbackMap.get(questionId);
            boolean visible = feedback != null && feedback.referenceAnswerVisible() != null
                    ? feedback.referenceAnswerVisible()
                    : autoGradable;
            result.add(TaskResultVO.QuestionInfo.builder()
                    .id(questionId)
                    .index(i + 1)
                    .type(String.valueOf(question.getOrDefault("type", "")))
                    .stem(questionStem(question))
                    .autoGrade(autoGradable)
                    .referenceAnswerVisible(visible)
                    .referenceAnswer(visible ? question.get("answer") : null)
                    .build());
        }
        return result;
    }

    private List<TaskResultVO.QuestionResult> buildQuestionResults(
            List<Map<String, Object>> questions,
            Map<String, Object> answers,
            Map<String, List<DimensionScore>> scoresByQuestion,
            Map<String, QuestionFeedbackItem> feedbackMap) {
        List<TaskResultVO.QuestionResult> result = new ArrayList<>();
        for (Map<String, Object> question : questions) {
            String questionId = questionId(question);
            List<DimensionScore> scores = scoresByQuestion.getOrDefault(questionId, List.of());
            boolean autoGradable = isAutoGradable(question);
            Object answer = answers.get(questionId);
            QuestionFeedbackItem feedback = feedbackMap.get(questionId);
            result.add(TaskResultVO.QuestionResult.builder()
                    .questionId(questionId)
                    .correct(autoGradable ? answersEqual(question.get("answer"), answer) : null)
                    .autoGraded(!scores.isEmpty() && scores.stream().allMatch(score -> Boolean.TRUE.equals(score.getAutoGraded())))
                    .earnedScore(sumScores(scores, true))
                    .maxScore(sumScores(scores, false))
                    .comment(feedback != null ? feedback.comment() : null)
                    .dimensionScores(scores.stream()
                            .map(score -> TaskResultVO.DimensionScoreResult.builder()
                                    .dimension(score.getDimension())
                                    .earnedScore(score.getEarnedScore())
                                    .maxScore(score.getMaxScore())
                                    .build())
                            .toList())
                    .build());
        }
        return result;
    }

    private List<TaskResultVO.DimensionSummary> buildDimensionSummary(List<DimensionScore> scores) {
        return scores.stream()
                .collect(Collectors.groupingBy(DimensionScore::getDimension))
                .entrySet().stream()
                .map(entry -> {
                    BigDecimal earned = sumScores(entry.getValue(), true);
                    BigDecimal max = sumScores(entry.getValue(), false);
                    BigDecimal rate = max.compareTo(BigDecimal.ZERO) > 0
                            ? earned.divide(max, 4, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return TaskResultVO.DimensionSummary.builder()
                            .dimension(entry.getKey())
                            .earnedScore(earned)
                            .maxScore(max)
                            .rate(rate)
                            .grade(gradeForRate(rate))
                            .build();
                })
                .sorted(Comparator.comparing(TaskResultVO.DimensionSummary::getDimension))
                .toList();
    }

    private List<Map<String, Object>> parseQuestions(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) return List.of();
        try {
            Map<String, Object> schema = JSON.readValue(schemaJson, new TypeReference<>() {});
            Object questions = schema.get("questions");
            if (!(questions instanceof List<?>)) questions = schema.get("fields");
            if (!(questions instanceof List<?> list)) return List.of();
            return list.stream()
                    .map(this::asStringObjectMap)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.debug("Task result ignored invalid schema", e);
            return List.of();
        }
    }

    private Map<String, Object> asStringObjectMap(Object value) {
        if (!(value instanceof Map<?, ?> raw)) return null;
        Map<String, Object> normalized = new LinkedHashMap<>();
        raw.forEach((key, item) -> normalized.put(String.valueOf(key), item));
        if (!normalized.containsKey("stem") && normalized.containsKey("label")) {
            normalized.put("stem", normalized.get("label"));
        }
        Object type = normalized.get("type");
        if ("radio".equals(type)) normalized.put("type", "single");
        if ("checkbox".equals(type)) normalized.put("type", "multiple");
        if ("textarea".equals(type)) normalized.put("type", "short");
        return normalized;
    }

    private Map<String, Object> parseAnswerMap(String content) {
        if (content == null || content.isBlank()) return Map.of();
        try {
            return JSON.readValue(content, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<String, QuestionFeedbackItem> parseQuestionFeedback(SubmissionFeedback feedback) {
        if (feedback == null || feedback.getQuestionFeedback() == null || feedback.getQuestionFeedback().isBlank()) {
            return Map.of();
        }
        try {
            List<QuestionFeedbackItem> items = JSON.readValue(feedback.getQuestionFeedback(), new TypeReference<>() {});
            return items.stream()
                    .filter(item -> item.questionId() != null && !item.questionId().isBlank())
                    .collect(Collectors.toMap(QuestionFeedbackItem::questionId, item -> item, (left, right) -> right));
        } catch (Exception e) {
            log.warn("Ignored invalid question feedback: submissionId={}", feedback.getSubmissionId(), e);
            return Map.of();
        }
    }

    private String questionStem(Map<String, Object> question) {
        Object stem = question.get("stem");
        if (stem instanceof String text && !text.isBlank()) return text;
        return Stream.of(question.get("title"), question.get("markdown"))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(text -> !text.isBlank())
                .collect(Collectors.joining("\n\n"));
    }

    private boolean answersEqual(Object expected, Object actual) {
        if (expected instanceof Collection<?> || actual instanceof Collection<?>) {
            Collection<?> left = expected instanceof Collection<?> collection ? collection : List.of(expected);
            Collection<?> right = actual instanceof Collection<?> collection ? collection : List.of(actual);
            return left.stream().map(String::valueOf).collect(Collectors.toSet())
                    .equals(right.stream().map(String::valueOf).collect(Collectors.toSet()));
        }
        if (expected instanceof Boolean expectedBool) {
            if (actual instanceof Boolean actualBool) return expectedBool.equals(actualBool);
            return expectedBool.toString().equalsIgnoreCase(String.valueOf(actual));
        }
        return Objects.equals(String.valueOf(expected).trim(), String.valueOf(actual).trim());
    }

    private String questionId(Map<String, Object> question) {
        return String.valueOf(question.getOrDefault("id", ""));
    }

    private boolean isAutoGradable(Map<String, Object> question) {
        return Boolean.TRUE.equals(question.get("autoGrade")) && question.get("answer") != null;
    }

    private BigDecimal sumScores(List<DimensionScore> scores, boolean earned) {
        return scores.stream()
                .map(score -> earned ? score.getEarnedScore() : score.getMaxScore())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String gradeForRate(BigDecimal rate) {
        if (rate.compareTo(new BigDecimal("0.9")) >= 0) return "A";
        if (rate.compareTo(new BigDecimal("0.8")) >= 0) return "B";
        if (rate.compareTo(new BigDecimal("0.6")) >= 0) return "C";
        if (rate.compareTo(new BigDecimal("0.4")) >= 0) return "D";
        if (rate.compareTo(BigDecimal.ZERO) > 0) return "E";
        return "F";
    }

    private record QuestionFeedbackItem(String questionId, String comment, Boolean referenceAnswerVisible) {}
}
