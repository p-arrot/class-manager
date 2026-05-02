package com.example.edu.modules.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "更新课程请求")
public class CourseUpdateDTO {

    @Schema(description = "课程名称", example = "Python编程基础")
    private String name;

    @Schema(description = "课程介绍")
    private String description;

    @Schema(description = "课程封面URL")
    private String coverUrl;

    @Schema(description = "授课班级ID列表")
    private List<Long> classIds;
}
