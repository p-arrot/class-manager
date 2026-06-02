package com.example.edu.modules.project.vo;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @Builder
public class ProjectVO {
    private Long id; private String name; private String description;
    private Long semesterId; private Integer maxTeamSize;
    private LocalDateTime deadline; private BigDecimal weight;
    private LocalDateTime createdAt;
}
