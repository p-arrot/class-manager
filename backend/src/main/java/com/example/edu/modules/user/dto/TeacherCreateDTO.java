package com.example.edu.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建教师请求")
public class TeacherCreateDTO {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", example = "teacher01")
    private String username;

    @NotBlank(message = "姓名不能为空")
    @Schema(description = "姓名", example = "张老师")
    private String name;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "123456")
    private String password;
}
