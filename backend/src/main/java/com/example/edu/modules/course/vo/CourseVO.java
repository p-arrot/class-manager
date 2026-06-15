package com.example.edu.modules.course.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "课程列表项")
public class CourseVO {

    @Schema(description = "课程ID")
    private Long id;

    @Schema(description = "课程名称")
    private String name;

    @Schema(description = "课程介绍")
    private String description;

    @Schema(description = "课程封面URL")
    private String coverUrl;

    @Schema(description = "创建教师ID")
    private Long teacherId;

    @Schema(description = "创建教师姓名")
    private String teacherName;

    @Schema(description = "关联班级数")
    private Integer classCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "学生已完成任务数(仅学生视角)")
    private Integer mySubmittedCount;

    @Schema(description = "总任务数(仅学生视角)")
    private Integer myTotalTaskCount;
}
