package com.example.edu.modules.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "学生提交")
public class SubmissionDTO {

    @NotBlank(message = "提交内容不能为空")
    @Schema(description = "提交内容 JSON（学习单答案/作品文件列表）")
    private String content;
}
