package com.example.edu.modules.exam.vo;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime;
@Data @Builder
public class ExamSubmissionVO {
    private Long id; private Long examId; private Long studentId;
    private String studentName; private String studentNo;
    private String answers; private Integer score; private String status;
    private LocalDateTime submittedAt; private LocalDateTime createdAt;
}
