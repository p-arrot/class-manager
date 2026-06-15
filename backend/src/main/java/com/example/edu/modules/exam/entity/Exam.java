package com.example.edu.modules.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @TableName("exams")
public class Exam {
    @TableId(type = IdType.AUTO) private Long id;
    private String name;
    private Long semesterId;
    private Long paperId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal weight;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
}
