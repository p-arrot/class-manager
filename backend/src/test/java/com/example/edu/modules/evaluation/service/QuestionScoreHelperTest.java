package com.example.edu.modules.evaluation.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionScoreHelperTest {

    private final QuestionScoreHelper helper = new QuestionScoreHelper();

    @Test
    void autoGradeWritesDimensionScoresForCorrectAnswer() {
        String schema = """
                {
                  "version": 3,
                  "questions": [
                    {
                      "id": "q1",
                      "type": "single",
                      "autoGrade": true,
                      "answer": "A",
                      "dimensionScores": [
                        {"dimension": "AWARENESS", "maxScore": 2},
                        {"dimension": "COMPUTING", "maxScore": 5},
                        {"dimension": "RESPONSIBILITY", "maxScore": 0}
                      ]
                    }
                  ]
                }
                """;

        List<DimensionScoreService.ScoreInput> scores = helper.autoGrade(schema, "{\"q1\":\"A\"}");

        assertThat(scores).hasSize(2);
        assertThat(scores).extracting(DimensionScoreService.ScoreInput::questionId)
                .containsOnly("q1");
        assertThat(scores).anySatisfy(score -> {
            assertThat(score.dimension()).isEqualTo("AWARENESS");
            assertThat(score.earnedScore()).isEqualByComparingTo(BigDecimal.valueOf(2));
            assertThat(score.maxScore()).isEqualByComparingTo(BigDecimal.valueOf(2));
            assertThat(score.autoGraded()).isTrue();
        });
        assertThat(scores).anySatisfy(score -> {
            assertThat(score.dimension()).isEqualTo("COMPUTING");
            assertThat(score.earnedScore()).isEqualByComparingTo(BigDecimal.valueOf(5));
            assertThat(score.maxScore()).isEqualByComparingTo(BigDecimal.valueOf(5));
        });
    }

    @Test
    void autoGradeKeepsMaxScoreButZeroesEarnedScoreForWrongAnswer() {
        String schema = """
                {
                  "version": 3,
                  "questions": [
                    {
                      "id": "q1",
                      "autoGrade": true,
                      "answer": "A",
                      "dimensionScores": [{"dimension": "COMPUTING", "maxScore": 3}]
                    }
                  ]
                }
                """;

        List<DimensionScoreService.ScoreInput> scores = helper.autoGrade(schema, "{\"q1\":\"B\"}");

        assertThat(scores).singleElement().satisfies(score -> {
            assertThat(score.earnedScore()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(score.maxScore()).isEqualByComparingTo(BigDecimal.valueOf(3));
        });
    }

    @Test
    void autoGradeComparesMultipleChoiceAnswersWithoutOrderSensitivity() {
        String schema = """
                {
                  "version": 3,
                  "questions": [
                    {
                      "id": "q1",
                      "autoGrade": true,
                      "answer": ["A", "C"],
                      "dimensionScores": [{"dimension": "DIGITAL_LEARNING", "maxScore": 4}]
                    }
                  ]
                }
                """;

        List<DimensionScoreService.ScoreInput> scores = helper.autoGrade(schema, "{\"q1\":[\"C\",\"A\"]}");

        assertThat(scores).singleElement().satisfies(score ->
                assertThat(score.earnedScore()).isEqualByComparingTo(BigDecimal.valueOf(4)));
    }

    @Test
    void autoGradeReturnsEmptyListForInvalidInput() {
        assertThat(helper.autoGrade("{bad-json", "{}")).isEmpty();
        assertThat(helper.autoGrade(null, "{}")).isEmpty();
        assertThat(helper.autoGrade("{\"questions\":[]}", null)).isEmpty();
    }
}
