package com.example.edu.modules.evaluation.enums;

public enum Grade {
    A(100), B(80), C(60), D(40), E(20), F(0);

    private final int score;
    Grade(int score) { this.score = score; }
    public int getScore() { return score; }
    public static int scoreOf(String grade) {
        try { return valueOf(grade).score; } catch (Exception e) { return 0; }
    }
}
