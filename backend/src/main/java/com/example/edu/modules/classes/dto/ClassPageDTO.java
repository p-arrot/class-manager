package com.example.edu.modules.classes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "班级分页查询参数")
public class ClassPageDTO {

    @Schema(description = "年级筛选", example = "三年级")
    private String grade;

    @Schema(description = "关键词搜索（班级名称）", example = "1班")
    private String keyword;

    @Schema(description = "页码", example = "1")
    private long page = 1;

    @Schema(description = "每页条数", example = "20")
    private long size = 20;
}
