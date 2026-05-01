package com.example.edu.modules.classes.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("school_classes")
public class SchoolClass {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学校ID（保留字段，当前不使用） */
    private Long schoolId;

    private String grade;

    private String name;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
