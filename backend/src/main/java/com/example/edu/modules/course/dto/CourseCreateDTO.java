package com.example.edu.modules.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "创建课程请求")
public class CourseCreateDTO {

    @NotBlank(message = "课程名称不能为空")
    @Schema(description = "课程名称", example = "Python编程基础")
    private String name;

    @Schema(description = "课程介绍", example = "面向初学者的Python编程入门课程")
    private String description;

    @Schema(description = "课程封面URL")
    private String coverUrl;

    @Schema(description = "授课班级ID列表")
    private List<Long> classIds;
}
