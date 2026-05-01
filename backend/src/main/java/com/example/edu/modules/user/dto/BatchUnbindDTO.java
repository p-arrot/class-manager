package com.example.edu.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量解绑班级请求")
public class BatchUnbindDTO {

    @NotEmpty(message = "班级ID列表不能为空")
    @Schema(description = "班级ID列表", example = "[1, 2]")
    private List<Long> classIds;
}
