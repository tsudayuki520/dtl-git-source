package com.dlust.sportbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RegistrationVO {
    private Long id;
    private Long participantId;
    private Long eventId;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // 关联字段
    private String participantName;
    private String eventName;
}
