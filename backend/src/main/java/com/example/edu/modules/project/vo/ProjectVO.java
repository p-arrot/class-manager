package com.example.edu.modules.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "项目")
public class ProjectVO {
    @Schema(description = "项目ID") private Long id;
    @Schema(description = "项目名称") private String name;
    @Schema(description = "描述") private String description;
    @Schema(description = "所属学期ID") private Long semesterId;
    @Schema(description = "截止时间") private LocalDateTime deadline;
    @Schema(description = "权重") private BigDecimal weight;
    @Schema(description = "创建时间") private LocalDateTime createdAt;
    @Schema(description = "当前学生提交ID") private Long submissionId;
    @Schema(description = "当前学生提交状态") private String submissionStatus;
    @Schema(description = "当前学生提交时间") private LocalDateTime submittedAt;
    @Schema(description = "当前学生得分") private BigDecimal score;
    @Schema(description = "退回原因") private String returnReason;
    @Schema(description = "是否允许再次提交") private Boolean canResubmit;
}
