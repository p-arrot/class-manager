package com.example.edu.modules.user.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class StudentExcelRowDTO {

    @ExcelProperty("年级")
    private String grade;

    @ExcelProperty("班级")
    private String className;

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("姓名")
    private String name;
}
