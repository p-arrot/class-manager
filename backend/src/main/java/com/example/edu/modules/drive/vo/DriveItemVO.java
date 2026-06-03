package com.example.edu.modules.drive.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "网盘文件/文件夹")
public class DriveItemVO {
    @Schema(description = "ID")
    private Long id;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "类型: FOLDER/FILE")
    private String type;
    @Schema(description = "文件大小(字节)")
    private Long fileSize;
    @Schema(description = "MIME 类型")
    private String contentType;
    @Schema(description = "父文件夹ID")
    private Long parentId;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
