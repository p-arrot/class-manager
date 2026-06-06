package com.example.edu.modules.evaluation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "评价")
public class EvaluationVO {
    @Schema(description = "维度: AWARENESS/COMPUTING/DIGITAL_LEARNING/RESPONSIBILITY") private String dimension;
    @Schema(description = "等级: A/B/C/D/E/F") private String grade;
    @Schema(description = "分数") private Integer score;
    @Schema(description = "维度中文标签") private String label;
}
