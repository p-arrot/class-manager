package com.example.edu.modules.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "学生信息")
public class StudentVO {

    @Schema(description = "用户ID", example = "10")
    private Long id;

    @Schema(description = "学号", example = "2024001")
    private String studentNo;

    @Schema(description = "姓名", example = "张三")
    private String name;

    @Schema(description = "班级ID", example = "1")
    private Long classId;

    @Schema(description = "年级", example = "三年级")
    private String grade;

    @Schema(description = "班级名称", example = "1班")
    private String className;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "邮箱", example = "student@example.com")
    private String email;

    @Schema(description = "启用状态", example = "true")
    private Boolean enabled;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
