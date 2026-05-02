package com.example.edu.modules.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_classes")
public class CourseClass {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long classId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
