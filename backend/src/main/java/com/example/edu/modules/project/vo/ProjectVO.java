package com.example.edu.modules.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "项目")
public class ProjectVO {
    @Schema(description = "项目ID") private Long id;
    @Schema(description = "项目名称") private String name;
    @Schema(description = "描述") private String description;
    @Schema(description = "所属学期ID") private Long semesterId;
    @Schema(description = "最大组员数") private Integer maxTeamSize;
    @Schema(description = "截止时间") private LocalDateTime deadline;
    @Schema(description = "权重") private BigDecimal weight;
    @Schema(description = "创建时间") private LocalDateTime createdAt;
}
