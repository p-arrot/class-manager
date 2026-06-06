package com.example.edu.modules.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "考试提交")
public class ExamSubmissionVO {
    @Schema(description = "提交ID") private Long id;
    @Schema(description = "考试ID") private Long examId;
    @Schema(description = "学生ID") private Long studentId;
    @Schema(description = "学生姓名") private String studentName;
    @Schema(description = "学号") private String studentNo;
    @Schema(description = "答案JSON") private String answers;
    @Schema(description = "得分") private Integer score;
    @Schema(description = "状态") private String status;
    @Schema(description = "提交时间") private LocalDateTime submittedAt;
    @Schema(description = "创建时间") private LocalDateTime createdAt;
}
