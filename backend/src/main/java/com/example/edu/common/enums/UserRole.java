package com.example.edu.common.enums;

public enum UserRole {
    ADMIN("admin"),
    TEACHER("teacher"),
    STUDENT("student");

    private final String code;
    UserRole(String code) { this.code = code; }
    public String getCode() { return code; }
}
