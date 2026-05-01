package com.example.edu.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "教师分页查询参数")
public class TeacherPageDTO {

    @Schema(description = "关键词搜索（用户名或姓名）", example = "张")
    private String keyword;

    @Schema(description = "页码", example = "1")
    private long page = 1;

    @Schema(description = "每页条数", example = "20")
    private long size = 20;
}
