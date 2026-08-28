package com.example.edu.modules.project.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project_submissions")
public class ProjectSubmission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long studentId;
    private String content;
    private String status;
    private LocalDateTime submittedAt;
    private String returnReason;
    private LocalDateTime returnedAt;
    private Integer revisionCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
