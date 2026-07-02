package com.dlust.sportbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String userCode;   // 学号/工号，登录账号（全局唯一）
    private String password;   // BCrypt 哈希
    private String name;
    private String gender;
    private String phone;
    private String college;
    private String major;      // 专业/单位
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
