package com.example.edu.modules.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "试卷")
public class ExamPaperVO {
    @Schema(description = "试卷ID") private Long id;
    @Schema(description = "标题") private String title;
    @Schema(description = "试卷内容JSON") private String content;
    @Schema(description = "总分") private Integer totalScore;
    @Schema(description = "出题教师ID") private Long teacherId;
    @Schema(description = "创建时间") private LocalDateTime createdAt;
    @Schema(description = "更新时间") private LocalDateTime updatedAt;
}
