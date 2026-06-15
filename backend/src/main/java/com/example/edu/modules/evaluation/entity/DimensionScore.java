package com.example.edu.modules.evaluation.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("dimension_scores")
public class DimensionScore {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String sourceType;
    private Long sourceId;
    private String questionId;
    private String dimension;
    private BigDecimal earnedScore;
    private BigDecimal maxScore;
    private Boolean autoGraded;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
