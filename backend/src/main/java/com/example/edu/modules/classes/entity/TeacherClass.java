package com.example.edu.modules.classes.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("teacher_classes")
public class TeacherClass {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teacherId;

    private Long classId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
