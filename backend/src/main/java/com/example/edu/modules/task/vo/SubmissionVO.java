package com.example.edu.modules.task.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SubmissionVO {
    private Long id;
    private Long taskId;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private String status;
    private String content;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
}
