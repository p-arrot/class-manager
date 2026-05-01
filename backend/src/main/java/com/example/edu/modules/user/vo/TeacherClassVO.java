package com.example.edu.modules.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "教师关联班级信息")
public class TeacherClassVO {

    @Schema(description = "关联记录ID", example = "1")
    private Long id;

    @Schema(description = "班级ID", example = "1")
    private Long classId;

    @Schema(description = "年级", example = "三年级")
    private String grade;

    @Schema(description = "班级名称", example = "1班")
    private String className;

    @Schema(description = "绑定时间")
    private LocalDateTime createdAt;
}
