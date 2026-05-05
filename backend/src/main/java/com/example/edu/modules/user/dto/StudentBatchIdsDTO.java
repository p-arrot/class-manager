package com.example.edu.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量操作请求")
public class StudentBatchIdsDTO {

    @NotEmpty(message = "学生ID列表不能为空")
    @Schema(description = "学生ID列表")
    private List<Long> ids;

    @Schema(description = "新密码，不传则重置为默认密码")
    private String newPassword;
}
