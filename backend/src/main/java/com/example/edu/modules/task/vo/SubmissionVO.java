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
    @Schema(description = "任务标题") private String taskTitle;
    @Schema(description = "任务类型: worksheet/artifact") private String taskType;
    @Schema(description = "学生ID") private Long studentId;
    @Schema(description = "学生姓名") private String studentName;
    @Schema(description = "学号") private String studentNo;
    @Schema(description = "状态: submitted/graded/returned/special") private String status;
    @Schema(description = "是否允许再次提交") private Boolean canResubmit;
    @Schema(description = "退回原因") private String returnReason;
    @Schema(description = "退回时间") private LocalDateTime returnedAt;
    @Schema(description = "修改次数") private Integer revisionCount;
    @Schema(description = "提交内容") private String content;
    @Schema(description = "提交时间") private LocalDateTime submittedAt;
    @Schema(description = "创建时间") private LocalDateTime createdAt;
}
