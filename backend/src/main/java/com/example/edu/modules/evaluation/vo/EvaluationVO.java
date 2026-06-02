package com.example.edu.modules.evaluation.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvaluationVO {
    private String dimension;
    private String grade;
    private Integer score;
    private String label;
}
