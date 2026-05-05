package com.example.edu.modules.course.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "课程资源节点（树形结构）")
public class CourseResourceVO {

    @Schema(description = "资源ID")
    private Long id;

    @Schema(description = "资源名称")
    private String name;

    @Schema(description = "所属课程ID")
    private Long courseId;

    @Schema(description = "父节点ID")
    private Long parentId;

    @Schema(description = "资源类型", example = "FOLDER")
    private String type;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "文件 MIME 类型")
    private String contentType;

    @Schema(description = "子节点列表")
    private List<CourseResourceVO> children;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
