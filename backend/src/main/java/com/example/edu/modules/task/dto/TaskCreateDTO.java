package com.example.edu.modules.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建课堂任务")
public class TaskCreateDTO {

    @NotBlank(message = "任务标题不能为空")
    @Schema(description = "任务标题", example = "Python基础选择题")
    private String title;

    @NotBlank(message = "任务类型不能为空")
    @Schema(description = "任务类型", example = "worksheet", allowableValues = {"worksheet", "artifact"})
    private String type;

    @Schema(description = "学习单 JSON Schema（仅worksheet类型）")
    private String formSchema;

    @Schema(description = "任务说明")
    private String description;

    @Schema(description = "截止时间")
    private String deadline;
}
