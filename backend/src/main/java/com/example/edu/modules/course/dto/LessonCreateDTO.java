package com.example.edu.modules.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建课时请求")
public class LessonCreateDTO {

    @NotBlank(message = "课时名称不能为空")
    @Schema(description = "课时名称", example = "第一课：认识Python")
    private String name;
}
