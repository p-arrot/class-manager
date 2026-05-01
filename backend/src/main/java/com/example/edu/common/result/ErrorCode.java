package com.example.edu.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(0, "ok"),

    // 400
    BAD_REQUEST(400, "请求参数错误"),
    VALIDATION_ERROR(40001, "参数校验失败"),
    FILE_PARSE_ERROR(40002, "文件解析失败"),
    FILE_EMPTY(40003, "文件为空"),
    EXCEL_FORMAT_ERROR(40004, "Excel格式错误或缺少必要列"),

    // 401
    UNAUTHORIZED(401, "未登录或登录已过期"),
    USERNAME_PASSWORD_ERROR(40101, "用户名或密码错误"),
    STUDENT_NO_PASSWORD_ERROR(40102, "学号或密码错误"),
    ACCOUNT_DISABLED(40103, "账号已被禁用"),

    // 403
    FORBIDDEN(403, "权限不足"),
    TEACHER_NOT_IN_CHARGE(40301, "您不负责该班级"),

    // 404
    NOT_FOUND(404, "资源不存在"),
    USER_NOT_FOUND(40401, "用户不存在"),
    CLASS_NOT_FOUND(40402, "班级不存在"),
    TEACHER_NOT_FOUND(40403, "教师不存在"),
    STUDENT_NOT_FOUND(40404, "学生不存在"),

    // 409
    CONFLICT(409, "数据冲突"),
    STUDENT_NO_DUPLICATE(40901, "学号已存在"),
    CLASS_NAME_DUPLICATE(40902, "班级名称已存在"),
    CLASS_HAS_STUDENTS(40903, "班级下还有学生，无法删除"),
    CLASS_HAS_TEACHERS(40904, "班级还有关联教师，无法删除"),
    USERNAME_DUPLICATE(40905, "用户名已存在"),

    // 500
    INTERNAL_ERROR(500, "服务器内部错误"),
    MINIO_ERROR(50001, "文件服务异常"),
    FILE_UPLOAD_ERROR(50002, "文件上传失败"),
    PASSWORD_RESET_FAILED(50003, "密码重置失败");

    private final int code;
    private final String msg;
}
