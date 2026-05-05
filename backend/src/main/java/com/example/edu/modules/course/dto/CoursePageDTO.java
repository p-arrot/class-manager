package com.example.edu.modules.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "课程分页查询")
public class CoursePageDTO {

    @Schema(description = "关键字（按课程名称搜索）")
    private String keyword;

    @Schema(description = "按班级ID筛选（教师端班级选择器联动）")
    private Long classId;

    @Schema(description = "页码", example = "1")
    private int page = 1;

    @Schema(description = "每页条数", example = "20")
    private int size = 20;
}
