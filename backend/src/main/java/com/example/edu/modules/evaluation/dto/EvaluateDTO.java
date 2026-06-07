package com.example.edu.modules.evaluation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "教师评分请求")
public class EvaluateDTO {

    @Schema(description = "各维度评分（特殊情况时可为空）")
    private List<DimensionGrade> dimensions;

    @Schema(description = "逐题各维度数值得分")
    private List<QuestionDimensionScore> questionScores;

    @Data
    @Schema(description = "单维度评分")
    public static class DimensionGrade {
        @Schema(description = "维度: AWARENESS/COMPUTING/DIGITAL_LEARNING/RESPONSIBILITY")
        private String dimension;
        @Schema(description = "等级: A/B/C/D/E")
        private String grade;
    }

    @Data
    @Schema(description = "单题单维度数值得分")
    public static class QuestionDimensionScore {
        @Schema(description = "题目ID")
        private String questionId;
        @Schema(description = "维度: AWARENESS/COMPUTING/DIGITAL_LEARNING/RESPONSIBILITY")
        private String dimension;
        @Schema(description = "本次得分")
        private BigDecimal earnedScore;
        @Schema(description = "该维度满分")
        private BigDecimal maxScore;
        @Schema(description = "是否自动批改")
        private Boolean autoGraded;
    }

    @Schema(description = "标记为特殊情况")
    private Boolean isSpecial;
}
