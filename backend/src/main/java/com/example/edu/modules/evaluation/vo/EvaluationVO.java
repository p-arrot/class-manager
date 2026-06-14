package com.example.edu.modules.evaluation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "评价")
public class EvaluationVO {
    @Schema(description = "评价来源类型: worksheet/artifact/project") private String sourceType;
    @Schema(description = "评价来源ID，当前课堂任务为提交ID") private Long sourceId;
    @Schema(description = "提交ID") private Long submissionId;
    @Schema(description = "任务ID") private Long taskId;
    @Schema(description = "任务标题") private String taskTitle;
    @Schema(description = "任务提交状态") private String taskStatus;
    @Schema(description = "维度: AWARENESS/COMPUTING/DIGITAL_LEARNING/RESPONSIBILITY") private String dimension;
    @Schema(description = "等级: A/B/C/D/E/F") private String grade;
    @Schema(description = "分数") private Integer score;
    @Schema(description = "维度中文标签") private String label;
    @Schema(description = "评价创建时间") private LocalDateTime evaluatedAt;
}
