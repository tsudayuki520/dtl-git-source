package com.dlust.sportbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Participant {
    private Long id;
    private Long sportsMeetingId;
    private Long userId;       // 关联 user.id（替代原 userCode/name/...）
    private Long teamId;       // 代表队（后续分配）
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // 以下为 JOIN 查询时的扩展字段（非 participant 表字段）
    private String userCode;   // JOIN user
    private String name;       // JOIN user
    private String gender;     // JOIN user
    private String phone;      // JOIN user
    private String college;    // JOIN user
    private String major;      // JOIN user
    private String teamName;   // JOIN team
    private String sportsMeetingName;  // JOIN sports_meeting
}
