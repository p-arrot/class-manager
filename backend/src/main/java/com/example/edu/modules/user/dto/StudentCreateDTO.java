package com.example.edu.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建学生请求")
public class StudentCreateDTO {

    @NotBlank(message = "学号不能为空")
    @Schema(description = "学号（全局唯一）", example = "2025001")
    private String studentNo;

    @NotBlank(message = "姓名不能为空")
    @Schema(description = "姓名", example = "张三")
    private String name;

    @NotNull(message = "班级不能为空")
    @Schema(description = "班级ID")
    private Long classId;

    @Schema(description = "密码，默认123456")
    private String password;
}
