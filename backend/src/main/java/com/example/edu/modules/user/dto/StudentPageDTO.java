package com.example.edu.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "学生分页查询参数")
public class StudentPageDTO {

    @Schema(description = "班级ID筛选", example = "1")
    private Long classId;

    @Schema(description = "关键词搜索（姓名或学号）", example = "张三")
    private String keyword;

    @Schema(description = "页码", example = "1")
    private long page = 1;

    @Schema(description = "每页条数", example = "20")
    private long size = 20;
}
