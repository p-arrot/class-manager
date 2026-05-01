package com.example.edu.modules.classes.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "班级信息")
public class ClassVO {

    @Schema(description = "班级ID", example = "1")
    private Long id;

    @Schema(description = "年级", example = "三年级")
    private String grade;

    @Schema(description = "班级名称", example = "1班")
    private String name;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
