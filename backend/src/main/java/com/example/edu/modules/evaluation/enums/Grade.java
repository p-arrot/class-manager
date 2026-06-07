package com.example.edu.modules.evaluation.enums;

public enum Grade {
    A(100), B(80), C(60), D(40), E(20), F(0);

    private final int score;

    Grade(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }

    public static int scoreOf(String grade) {
        if (grade == null || grade.isBlank()) {
            return 0;
        }
        for (Grade value : values()) {
            if (value.name().equals(grade.trim())) {
                return value.score;
            }
        }
        return 0;
    }
}
