package com.example.edu.modules.evaluation.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RadarVO {
    private List<DimensionScore> current;
    private List<DimensionScore> previous;
    private boolean hasPrevious;

    @Data
    @Builder
    public static class DimensionScore {
        private String dimension;
        private String label;
        private Double avgScore;
    }
}
