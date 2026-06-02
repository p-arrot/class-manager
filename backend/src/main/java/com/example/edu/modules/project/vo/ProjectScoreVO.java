package com.example.edu.modules.project.vo;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime;
@Data @Builder
public class ProjectScoreVO {
    private Long id; private Long projectId; private Long studentId;
    private String studentName; private String studentNo;
    private String grade; private Integer isSpecial; private LocalDateTime createdAt;
}
