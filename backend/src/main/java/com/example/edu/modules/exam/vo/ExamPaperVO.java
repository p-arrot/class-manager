package com.example.edu.modules.exam.vo;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime;
@Data @Builder
public class ExamPaperVO {
    private Long id; private String title; private String content;
    private Integer totalScore; private Long teacherId;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
