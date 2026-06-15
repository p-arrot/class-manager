package com.example.edu.modules.task.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class TaskResultVO {
    private TaskInfo task;
    private String status;
    private SubmissionResult submission;
    private List<QuestionInfo> questions;
    private Map<String, Object> answers;
    private List<QuestionResult> questionResults;
    private List<DimensionSummary> dimensionSummary;

    @Data
    @Builder
    public static class TaskInfo {
        private Long id;
        private String title;
        private String type;
        private Long courseId;
        private String courseName;
        private Long lessonId;
        private String lessonName;
    }

    @Data
    @Builder
    public static class SubmissionResult {
        private Long id;
        private String status;
        private String content;
        private LocalDateTime submittedAt;
        private LocalDateTime gradedAt;
        private String teacherComment;
    }

    @Data
    @Builder
    public static class QuestionInfo {
        private String id;
        private Integer index;
        private String type;
        private String stem;
        private Boolean autoGrade;
        private Boolean referenceAnswerVisible;
        private Object referenceAnswer;
    }

    @Data
    @Builder
    public static class QuestionResult {
        private String questionId;
        private Boolean correct;
        private Boolean autoGraded;
        private BigDecimal earnedScore;
        private BigDecimal maxScore;
        private String comment;
        private List<DimensionScoreResult> dimensionScores;
    }

    @Data
    @Builder
    public static class DimensionScoreResult {
        private String dimension;
        private BigDecimal earnedScore;
        private BigDecimal maxScore;
    }

    @Data
    @Builder
    public static class DimensionSummary {
        private String dimension;
        private BigDecimal earnedScore;
        private BigDecimal maxScore;
        private BigDecimal rate;
        private String grade;
    }
}
