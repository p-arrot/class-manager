package com.example.edu.modules.classes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建班级请求")
public class ClassCreateDTO {

    @NotBlank(message = "年级不能为空")
    @Schema(description = "年级", example = "三年级")
    private String grade;

    @NotBlank(message = "班级名称不能为空")
    @Schema(description = "班级名称", example = "1班")
    private String name;
}
