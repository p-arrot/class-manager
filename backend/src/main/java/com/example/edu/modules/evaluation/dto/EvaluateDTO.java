package com.example.edu.modules.evaluation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "教师评分请求")
public class EvaluateDTO {

    @Schema(description = "各维度评分（特殊情况时可为空）")
    private List<DimensionGrade> dimensions;

    @Data
    @Schema(description = "单维度评分")
    public static class DimensionGrade {
        @Schema(description = "维度: AWARENESS/COMPUTING/DIGITAL_LEARNING/RESPONSIBILITY")
        private String dimension;
        @Schema(description = "等级: A/B/C/D/E")
        private String grade;
    }

    @Schema(description = "标记为特殊情况")
    private Boolean isSpecial;
}
