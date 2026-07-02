package com.dlust.sportbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RegistrationVO {
    private Long id;
    private Long participantId;
    private Long eventId;
    private Long scheduleId;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // 关联字段
    private String participantName;
    private String teamName;
    private String eventName;
    private String scheduleName;
    // 来自 JOIN user 表的字段（mapper 中 SELECT u.user_code/gender/phone/college/major）
    private String userCode;
    private String gender;
    private String phone;
    private String college;
    private String major;
}
