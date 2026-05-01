package com.example.edu.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "重置密码请求")
public class PasswordResetDTO {

    @Schema(description = "新密码（不传则重置为默认密码123456）", example = "123456")
    private String newPassword;
}
