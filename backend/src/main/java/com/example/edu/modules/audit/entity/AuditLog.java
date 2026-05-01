package com.example.edu.modules.audit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_logs")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String action;

    private String targetType;

    private Long targetId;

    private String detail;

    private String ip;

    private String userAgent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
