package com.example.edu.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "编辑学生请求")
public class StudentUpdateDTO {

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "班级ID")
    private Long classId;

    @Schema(description = "启用状态")
    private Boolean enabled;
}
