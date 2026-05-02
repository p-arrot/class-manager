package com.example.edu.modules.course.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "学期信息")
public class SemesterVO {

    @Schema(description = "学期ID")
    private Long id;

    @Schema(description = "学期名称")
    private String name;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "所属课程ID")
    private Long courseId;

    @Schema(description = "课时数")
    private Integer lessonCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
