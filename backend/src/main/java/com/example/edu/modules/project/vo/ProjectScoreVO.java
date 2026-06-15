package com.example.edu.modules.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "项目评分")
public class ProjectScoreVO {
    @Schema(description = "评分ID") private Long id;
    @Schema(description = "项目ID") private Long projectId;
    @Schema(description = "学生ID") private Long studentId;
    @Schema(description = "学生姓名") private String studentName;
    @Schema(description = "学号") private String studentNo;
    @Schema(description = "等级") private String grade;
    @Schema(description = "是否特殊情况") private Integer isSpecial;
    @Schema(description = "创建时间") private LocalDateTime createdAt;
}
