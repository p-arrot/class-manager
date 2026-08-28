package com.example.edu.modules.evaluation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.modules.evaluation.entity.DimensionScore;
import com.example.edu.modules.evaluation.mapper.DimensionScoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DimensionScoreService {

    public static final List<String> DIMENSIONS = List.of(
            "AWARENESS",
            "COMPUTING",
            "DIGITAL_LEARNING",
            "RESPONSIBILITY"
    );

    private final DimensionScoreMapper dimensionScoreMapper;

    @Transactional(rollbackFor = Exception.class)
    public void replaceScores(String sourceType, Long sourceId, Long studentId, List<ScoreInput> scores) {
        dimensionScoreMapper.delete(new LambdaQueryWrapper<DimensionScore>()
                .eq(DimensionScore::getSourceType, sourceType)
                .eq(DimensionScore::getSourceId, sourceId));
        insertScores(sourceType, sourceId, studentId, scores);
    }

    @Transactional(rollbackFor = Exception.class)
    public void replaceAutoScores(String sourceType, Long sourceId, Long studentId, List<ScoreInput> scores) {
        dimensionScoreMapper.delete(new LambdaQueryWrapper<DimensionScore>()
                .eq(DimensionScore::getSourceType, sourceType)
                .eq(DimensionScore::getSourceId, sourceId)
                .eq(DimensionScore::getAutoGraded, true));
        insertScores(sourceType, sourceId, studentId, scores);
    }

    public List<DimensionScore> listBySources(String sourceType, List<Long> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) return List.of();
        return dimensionScoreMapper.selectList(new LambdaQueryWrapper<DimensionScore>()
                .eq(DimensionScore::getSourceType, sourceType)
                .in(DimensionScore::getSourceId, sourceIds));
    }

    public List<DimensionScore> listByStudent(Long studentId) {
        return dimensionScoreMapper.selectList(new LambdaQueryWrapper<DimensionScore>()
                .eq(DimensionScore::getStudentId, studentId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void clearScores(String sourceType, Long sourceId) {
        dimensionScoreMapper.delete(new LambdaQueryWrapper<DimensionScore>()
                .eq(DimensionScore::getSourceType, sourceType)
                .eq(DimensionScore::getSourceId, sourceId));
    }

    private void insertScores(String sourceType, Long sourceId, Long studentId, List<ScoreInput> scores) {
        if (scores == null || scores.isEmpty()) return;
        for (ScoreInput input : scores) {
            if (input == null || input.maxScore() == null || input.maxScore().compareTo(BigDecimal.ZERO) <= 0) continue;
            if (!DIMENSIONS.contains(input.dimension())) continue;
            BigDecimal earned = input.earnedScore() == null ? BigDecimal.ZERO : input.earnedScore();
            if (earned.compareTo(BigDecimal.ZERO) < 0) earned = BigDecimal.ZERO;
            if (earned.compareTo(input.maxScore()) > 0) earned = input.maxScore();

            DimensionScore score = new DimensionScore();
            score.setStudentId(studentId);
            score.setSourceType(sourceType);
            score.setSourceId(sourceId);
            score.setQuestionId(input.questionId());
            score.setDimension(input.dimension());
            score.setEarnedScore(earned);
            score.setMaxScore(input.maxScore());
            score.setAutoGraded(Boolean.TRUE.equals(input.autoGraded()));
            dimensionScoreMapper.insert(score);
        }
    }

    public record ScoreInput(
            String questionId,
            String dimension,
            BigDecimal earnedScore,
            BigDecimal maxScore,
            Boolean autoGraded
    ) {
        public ScoreInput {
            Objects.requireNonNull(dimension, "dimension");
        }
    }
}
