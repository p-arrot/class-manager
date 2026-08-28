package com.example.edu.modules.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("exam_submissions")
public class ExamSubmission {
    @TableId(type = IdType.AUTO) private Long id;
    private Long examId;
    private Long studentId;
    private String answers;
    private Integer score;
    private String status;
    private LocalDateTime submittedAt;
    private String returnReason;
    private LocalDateTime returnedAt;
    private LocalDateTime startedAt;
    private LocalDateTime updatedAt;
    private Integer revisionCount;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
}
