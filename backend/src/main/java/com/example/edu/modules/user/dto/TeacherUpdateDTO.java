package com.example.edu.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "更新教师请求")
public class TeacherUpdateDTO {

    @NotBlank(message = "姓名不能为空")
    @Schema(description = "姓名", example = "张老师")
    private String name;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "邮箱", example = "teacher@example.com")
    private String email;

    @Schema(description = "启用状态", example = "true")
    private Boolean enabled;
}
