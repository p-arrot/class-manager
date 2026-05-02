package com.example.edu.modules.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建课程资源文件夹请求")
public class CourseResourceCreateDTO {

    @NotBlank(message = "文件夹名称不能为空")
    @Schema(description = "文件夹名称", example = "课件资料")
    private String name;

    @Schema(description = "父文件夹ID，为空则创建在根目录")
    private Long parentId;
}
