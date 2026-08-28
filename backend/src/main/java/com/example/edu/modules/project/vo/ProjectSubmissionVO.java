package com.example.edu.modules.project.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProjectSubmissionVO {
    private Long id;
    private Long submissionId;
    private Long projectId;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private Long classId;
    private String className;
    private String content;
    private String status;
    private Boolean canResubmit;
    private String returnReason;
    private LocalDateTime returnedAt;
    private Integer revisionCount;
    private BigDecimal score;
    private List<DimensionScoreVO> dimensionScores;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;

    public record DimensionScoreVO(
            String questionId,
            String dimension,
            BigDecimal earnedScore,
            BigDecimal maxScore
    ) {}
}
