package com.example.edu.modules.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "移动资源请求")
public class CourseResourceMoveDTO {

    @Schema(description = "目标父文件夹ID，为空则移动到根目录")
    private Long targetParentId;

    @Schema(description = "目标排序位置，为空则追加到末尾")
    private Integer targetSortOrder;
}
