package com.example.edu.modules.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "重命名资源请求")
public class CourseResourceUpdateDTO {

    @NotBlank(message = "名称不能为空")
    @Schema(description = "新名称", example = "新文件夹名")
    private String name;
}
