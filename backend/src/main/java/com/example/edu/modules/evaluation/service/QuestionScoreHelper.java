package com.example.edu.modules.evaluation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class QuestionScoreHelper {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<DimensionScoreService.ScoreInput> autoGrade(String schemaJson, String answerJson) {
        if (schemaJson == null || answerJson == null) return List.of();
        try {
            Map<String, Object> schema = mapper.readValue(schemaJson, new TypeReference<>() {});
            Object questionsObj = schema.get("questions");
            if (!(questionsObj instanceof List<?> questions)) return List.of();
            Map<String, Object> answers = mapper.readValue(answerJson, new TypeReference<>() {});
            List<DimensionScoreService.ScoreInput> scores = new ArrayList<>();

            for (Object item : questions) {
                if (!(item instanceof Map<?, ?> q)) continue;
                if (!Boolean.TRUE.equals(q.get("autoGrade"))) continue;
                Object id = q.get("id");
                if (id == null || !q.containsKey("answer")) continue;
                boolean correct = answersEqual(q.get("answer"), answers.get(String.valueOf(id)));
                for (DimensionConfig config : dimensionConfigs(q)) {
                    BigDecimal earned = correct ? config.maxScore() : BigDecimal.ZERO;
                    scores.add(new DimensionScoreService.ScoreInput(
                            String.valueOf(id),
                            config.dimension(),
                            earned,
                            config.maxScore(),
                            true
                    ));
                }
            }
            return scores;
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private List<DimensionConfig> dimensionConfigs(Map<?, ?> question) {
        Object configs = question.get("dimensionScores");
        if (configs instanceof List<?> list) {
            List<DimensionConfig> parsed = new ArrayList<>();
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> map)) continue;
                Object dimension = map.get("dimension");
                Object max = map.get("maxScore");
                BigDecimal maxScore = toBigDecimal(max);
                if (dimension != null && maxScore.compareTo(BigDecimal.ZERO) > 0) {
                    parsed.add(new DimensionConfig(String.valueOf(dimension), maxScore));
                }
            }
            if (!parsed.isEmpty()) return parsed;
        }

        BigDecimal legacyScore = toBigDecimal(question.get("score"));
        if (legacyScore.compareTo(BigDecimal.ZERO) <= 0) legacyScore = BigDecimal.ONE;
        return List.of(new DimensionConfig("COMPUTING", legacyScore));
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        if (value instanceof String s && !s.isBlank()) {
            try {
                return new BigDecimal(s.trim());
            } catch (NumberFormatException ignored) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    private boolean answersEqual(Object expected, Object actual) {
        if (expected instanceof List<?> expectedList) {
            if (!(actual instanceof List<?> actualList)) return false;
            List<String> e = expectedList.stream().map(String::valueOf).sorted(Comparator.naturalOrder()).toList();
            List<String> a = actualList.stream().map(String::valueOf).sorted(Comparator.naturalOrder()).toList();
            return e.equals(a);
        }
        return Objects.equals(String.valueOf(expected), String.valueOf(actual));
    }

    private record DimensionConfig(String dimension, BigDecimal maxScore) {}
}
