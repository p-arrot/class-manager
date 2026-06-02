package com.example.edu.modules.task.enums;

public enum TaskType {
    WORKSHEET("worksheet", "学习单"),
    ARTIFACT("artifact", "课堂作品");

    private final String code;
    private final String label;

    TaskType(String code, String label) { this.code = code; this.label = label; }
    public String getCode() { return code; }
    public String getLabel() { return label; }
}
