package com.example.edu.modules.project.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectSubmissionVO {
    private Long id;
    private Long submissionId;
    private Long projectId;
    private Long teamId;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private Long classId;
    private String className;
    private String content;
    private String status;
    private java.math.BigDecimal score;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
}
