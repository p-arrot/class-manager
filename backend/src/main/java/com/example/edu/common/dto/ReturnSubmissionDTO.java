package com.example.edu.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReturnSubmissionDTO(
        @NotBlank(message = "请填写退回原因")
        @Size(max = 500, message = "退回原因不能超过500字")
        String reason
) {}
