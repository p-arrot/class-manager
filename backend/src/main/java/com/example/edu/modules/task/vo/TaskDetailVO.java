package com.example.edu.modules.task.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskDetailVO {
    private Long id;
    private String title;
    private String type;
    private Long lessonId;
    private String formSchema;
    private String description;
    private LocalDateTime deadline;
    private Integer submissionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
