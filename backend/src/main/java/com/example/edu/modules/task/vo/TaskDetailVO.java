package com.example.edu.modules.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "任务详情")
public class TaskDetailVO {
    @Schema(description = "任务ID") private Long id;
    @Schema(description = "标题") private String title;
    @Schema(description = "类型: worksheet/artifact") private String type;
    @Schema(description = "所属课时ID") private Long lessonId;
    @Schema(description = "表单模板JSON") private String formSchema;
    @Schema(description = "描述") private String description;
    @Schema(description = "截止时间") private LocalDateTime deadline;
    @Schema(description = "提交数") private Integer submissionCount;
    @Schema(description = "创建时间") private LocalDateTime createdAt;
    @Schema(description = "更新时间") private LocalDateTime updatedAt;
}
