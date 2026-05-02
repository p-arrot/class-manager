package com.example.edu.modules.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "课时排序请求")
public class LessonSortDTO {

    @NotNull(message = "目标位置不能为空")
    @Schema(description = "目标位置（从0开始）", example = "0")
    private Integer targetIndex;
}
