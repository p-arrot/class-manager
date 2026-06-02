package com.example.edu.modules.evaluation.enums;

public enum EvaluationDimension {
    AWARENESS("信息意识"),
    COMPUTING("计算思维"),
    DIGITAL_LEARNING("数字化学习与创新"),
    RESPONSIBILITY("信息社会责任");

    private final String label;
    EvaluationDimension(String label) { this.label = label; }
    public String getLabel() { return label; }
}
