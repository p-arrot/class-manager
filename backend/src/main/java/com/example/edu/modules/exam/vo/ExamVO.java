package com.example.edu.modules.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "考试")
public class ExamVO {
    @Schema(description = "考试ID") private Long id;
    @Schema(description = "考试名称") private String name;
    @Schema(description = "所属学期ID") private Long semesterId;
    @Schema(description = "试卷ID") private Long paperId;
    @Schema(description = "试卷内容JSON") private String paperContent;
    @Schema(description = "开始时间") private LocalDateTime startTime;
    @Schema(description = "结束时间") private LocalDateTime endTime;
    @Schema(description = "权重") private BigDecimal weight;
    @Schema(description = "创建时间") private LocalDateTime createdAt;
    @Schema(description = "当前学生提交ID") private Long submissionId;
    @Schema(description = "当前学生提交状态") private String submissionStatus;
    @Schema(description = "当前学生提交时间") private LocalDateTime submittedAt;
    @Schema(description = "当前学生得分") private Integer score;
    @Schema(description = "退回原因") private String returnReason;
    @Schema(description = "是否允许再次提交") private Boolean canResubmit;
    @Schema(description = "开始答题时间") private LocalDateTime startedAt;
}
