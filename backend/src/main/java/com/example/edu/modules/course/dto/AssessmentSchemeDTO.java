package com.example.edu.modules.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssessmentSchemeDTO {
    @NotNull
    @Min(0)
    @Max(100)
    private Integer processPercent;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer examPercent;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer projectPercent;
}
