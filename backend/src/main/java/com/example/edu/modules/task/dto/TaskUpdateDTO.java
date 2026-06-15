package com.example.edu.modules.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "编辑课堂任务")
public class TaskUpdateDTO {

    @Schema(description = "任务标题")
    private String title;

    @Schema(description = "学习单 JSON Schema")
    private String formSchema;

    @Schema(description = "任务说明")
    private String description;

    @Schema(description = "截止时间")
    private String deadline;
}
