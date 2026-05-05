package com.example.edu.modules.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "文件上传预签名请求")
public class FileUploadDTO {

    @NotBlank(message = "文件名不能为空")
    @Schema(description = "原始文件名", example = "report.docx")
    private String fileName;

    @NotBlank(message = "文件类型不能为空")
    @Schema(description = "文件 MIME 类型", example = "application/pdf")
    private String contentType;

    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小必须大于 0")
    @Max(value = 209715200, message = "文件大小不能超过 200MB")
    @Schema(description = "文件大小（字节）", example = "1048576")
    private Long fileSize;

    @NotNull(message = "课程 ID 不能为空")
    @Schema(description = "所属课程 ID", example = "1")
    private Long courseId;

    @Schema(description = "父文件夹 ID，不传则上传到根目录")
    private Long parentId;
}
