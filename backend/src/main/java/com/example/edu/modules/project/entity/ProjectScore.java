package com.example.edu.modules.project.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("project_scores")
public class ProjectScore {
    @TableId(type = IdType.AUTO) private Long id;
    private Long projectId;
    private Long studentId;
    private String grade;
    private Integer isSpecial;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
}
