package com.example.edu.modules.course.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssessmentSchemeVO {
    private Long id;
    private Long semesterId;
    private Integer processPercent;
    private Integer examPercent;
    private Integer projectPercent;
}
