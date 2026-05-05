package com.example.edu.modules.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lesson_resources")
public class LessonResource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lessonId;
    private String name;
    private Long fileSize;
    private String contentType;
    private String objectName;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
