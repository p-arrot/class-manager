package com.example.edu.modules.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "创建学期请求")
public class SemesterCreateDTO {

    @NotBlank(message = "学期名称不能为空")
    @Schema(description = "学期名称", example = "2026年秋季学期")
    private String name;

    @NotNull(message = "开始时间不能为空")
    @Schema(description = "开始时间", example = "2026-09-01 00:00:00")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @Schema(description = "结束时间", example = "2027-01-15 00:00:00")
    private LocalDateTime endTime;
}
