package com.example.edu.modules.drive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("user_drive")
public class DriveItem {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Long parentId;
    private String name;
    private String type;
    private Long fileSize;
    private String contentType;
    private String objectName;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
}
