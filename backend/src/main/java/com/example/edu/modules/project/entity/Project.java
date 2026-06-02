package com.example.edu.modules.project.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @TableName("projects")
public class Project {
    @TableId(type = IdType.AUTO) private Long id;
    private String name;
    private String description;
    private Long semesterId;
    private Integer maxTeamSize;
    private LocalDateTime deadline;
    private BigDecimal weight;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
}
