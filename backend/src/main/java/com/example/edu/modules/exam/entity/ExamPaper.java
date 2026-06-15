package com.example.edu.modules.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("exam_papers")
public class ExamPaper {
    @TableId(type = IdType.AUTO) private Long id;
    private String title;
    private String content;
    private Integer totalScore;
    private Long teacherId;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
}
