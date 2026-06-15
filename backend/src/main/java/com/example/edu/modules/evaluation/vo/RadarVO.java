package com.example.edu.modules.evaluation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "学生雷达图数据")
public class RadarVO {
    @Schema(description = "当前学期各维度评分") private List<DimensionScore> current;
    @Schema(description = "上学期各维度评分") private List<DimensionScore> previous;
    @Schema(description = "是否存在上学期数据") private boolean hasPrevious;

    @Data
    @Builder
    @Schema(description = "维度得分")
    public static class DimensionScore {
        @Schema(description = "维度编码") private String dimension;
        @Schema(description = "维度中文标签") private String label;
        @Schema(description = "平均分") private Double avgScore;
    }
}
