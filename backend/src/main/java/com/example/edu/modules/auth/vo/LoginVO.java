package com.example.edu.modules.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "登录响应")
public class LoginVO {

    @Schema(description = "JWT令牌", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "用户名或学号", example = "admin")
    private String username;

    @Schema(description = "真实姓名", example = "系统管理员")
    private String name;

    @Schema(description = "角色", example = "admin")
    private String role;

    @Schema(description = "班级ID（学生有值）")
    private Long classId;
}
