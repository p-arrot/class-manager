package com.example.edu.modules.evaluation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("evaluations")
public class Evaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String sourceType;
    private Long sourceId;
    private String dimension;
    private String grade;
    private Integer isSpecial;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
