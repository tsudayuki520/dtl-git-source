package com.dlust.sportbackend.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ParticipantImportRow {
    @ExcelProperty("学号/工号") private String userCode;
    @ExcelProperty("姓名") private String name;
    @ExcelProperty("性别") private String gender;
    @ExcelProperty("电话") private String phone;
    @ExcelProperty("学院") private String college;
    @ExcelProperty("专业/单位") private String major;
}
