package com.example.edu.modules.task.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskVO {
    private Long id;
    private String title;
    private String type;
    private Long lessonId;
    private String description;
    private LocalDateTime deadline;
    private Integer submissionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
