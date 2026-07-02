package com.dlust.sportbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Participant {
    private Long id;
    private Long sportsMeetingId;
    private Long teamId;           // 代表队ID（关联team表）
    private String teamName;       // 代表队名称（JOIN查询）
    private String userCode;
    private String name;
    private String phone;
    private String gender;
    private String college;
    private String major;       // 专业/单位
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
