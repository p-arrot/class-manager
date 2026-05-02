package com.example.edu.modules.course.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "课时信息")
public class LessonVO {

    @Schema(description = "课时ID")
    private Long id;

    @Schema(description = "课时名称")
    private String name;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "所属学期ID")
    private Long semesterId;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
