package com.example.edu.modules.task.enums;

public enum SubmissionStatus {
    SUBMITTED("submitted", "已提交"),
    GRADED("graded", "已评分"),
    SPECIAL("special", "特殊情况");

    private final String code;
    private final String label;

    SubmissionStatus(String code, String label) { this.code = code; this.label = label; }
    public String getCode() { return code; }
    public String getLabel() { return label; }
}
