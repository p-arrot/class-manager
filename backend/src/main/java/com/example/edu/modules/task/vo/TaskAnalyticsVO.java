package com.example.edu.modules.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@Schema(description = "任务完成情况数据看板")
public class TaskAnalyticsVO {
    private Long taskId;
    private String title;
    private String type;
    private Integer totalStudents;
    private Integer submittedCount;
    private Integer gradedCount;
    private Integer specialCount;
    private Integer notSubmittedCount;
    private Double submissionRate;
    private Double accuracyRate;
    private List<QuestionAnalyticsVO> questions;
    private List<StudentTaskAnswerVO> submissions;

    @Data
    @Builder
    public static class QuestionAnalyticsVO {
        private String questionId;
        private Integer index;
        private String type;
        private String stem;
        private Boolean autoGradable;
        private Integer answerCount;
        private Integer correctCount;
        private Double accuracyRate;
        private Map<String, Integer> optionDistribution;
        private List<StudentAnswerVO> answers;
    }

    @Data
    @Builder
    public static class StudentAnswerVO {
        private Long submissionId;
        private Long studentId;
        private String studentName;
        private String studentNo;
        private String status;
        private Object answer;
        private Boolean correct;
        private LocalDateTime submittedAt;
    }

    @Data
    @Builder
    public static class StudentTaskAnswerVO {
        private Long submissionId;
        private Long studentId;
        private String studentName;
        private String studentNo;
        private String status;
        private String content;
        private LocalDateTime submittedAt;
    }
}
