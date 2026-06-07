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
    private Long teamId;
    private Long studentId;
    private String content;
    private LocalDateTime submittedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
