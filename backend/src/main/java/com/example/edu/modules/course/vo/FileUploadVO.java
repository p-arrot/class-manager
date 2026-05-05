package com.example.edu.modules.course.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "文件上传预签名响应")
public class FileUploadVO {

    @Schema(description = "新创建的 CourseResource ID")
    private Long resourceId;

    @Schema(description = "MinIO 预签名上传 URL")
    private String presignedUrl;
}
