package com.example.edu.modules.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "学生导入结果")
public class StudentImportResultVO {

    @Schema(description = "成功导入数量", example = "10")
    private int successCount;

    @Schema(description = "失败数量", example = "2")
    private int failCount;

    @Schema(description = "失败详情")
    private List<ImportError> errors;

    @Data
    @Builder
    @Schema(description = "导入错误详情")
    public static class ImportError {

        @Schema(description = "Excel行号", example = "3")
        private int rowNum;

        @Schema(description = "学号", example = "2024001")
        private String studentNo;

        @Schema(description = "姓名", example = "张三")
        private String name;

        @Schema(description = "错误原因", example = "学号已存在")
        private String errorMsg;
    }
}
