package com.example.edu.modules.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "教师信息")
public class TeacherVO {

    @Schema(description = "用户ID", example = "2")
    private Long id;

    @Schema(description = "用户名", example = "teacher01")
    private String username;

    @Schema(description = "姓名", example = "张老师")
    private String name;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "邮箱", example = "teacher@example.com")
    private String email;

    @Schema(description = "启用状态", example = "true")
    private Boolean enabled;

    @Schema(description = "负责班级ID列表", example = "[1, 2]")
    private List<Long> classIds;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
