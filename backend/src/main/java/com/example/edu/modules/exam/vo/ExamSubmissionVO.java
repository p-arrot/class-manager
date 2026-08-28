package com.example.edu.modules.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "考试提交")
public class ExamSubmissionVO {
    @Schema(description = "提交ID") private Long id;
    @Schema(description = "提交ID，未提交时为空") private Long submissionId;
    @Schema(description = "考试ID") private Long examId;
    @Schema(description = "学生ID") private Long studentId;
    @Schema(description = "学生姓名") private String studentName;
    @Schema(description = "学号") private String studentNo;
    @Schema(description = "班级ID") private Long classId;
    @Schema(description = "班级名称") private String className;
    @Schema(description = "答案JSON") private String answers;
    @Schema(description = "得分") private Integer score;
    @Schema(description = "状态") private String status;
    @Schema(description = "是否允许再次提交") private Boolean canResubmit;
    @Schema(description = "退回原因") private String returnReason;
    @Schema(description = "退回时间") private LocalDateTime returnedAt;
    @Schema(description = "开始答题时间") private LocalDateTime startedAt;
    @Schema(description = "修改次数") private Integer revisionCount;
    @Schema(description = "提交时间") private LocalDateTime submittedAt;
    @Schema(description = "创建时间") private LocalDateTime createdAt;
}
