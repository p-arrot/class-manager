package com.example.edu.modules.exam.vo;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @Builder
public class ExamVO {
    private Long id; private String name; private Long semesterId; private Long paperId;
    private LocalDateTime startTime; private LocalDateTime endTime;
    private BigDecimal weight; private LocalDateTime createdAt;
}
