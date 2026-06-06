package com.example.edu.modules.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "任务提交")
public class SubmissionVO {
    @Schema(description = "提交ID") private Long id;
    @Schema(description = "任务ID") private Long taskId;
    @Schema(description = "学生ID") private Long studentId;
    @Schema(description = "学生姓名") private String studentName;
    @Schema(description = "学号") private String studentNo;
    @Schema(description = "状态: submitted/graded/special") private String status;
    @Schema(description = "提交内容") private String content;
    @Schema(description = "提交时间") private LocalDateTime submittedAt;
    @Schema(description = "创建时间") private LocalDateTime createdAt;
}
