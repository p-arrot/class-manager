package com.example.edu.modules.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("assessment_schemes")
public class AssessmentScheme {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long semesterId;
    private Integer processPercent;
    private Integer examPercent;
    private Integer projectPercent;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
