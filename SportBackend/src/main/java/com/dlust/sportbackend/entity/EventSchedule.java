package com.dlust.sportbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventSchedule {
    private Long id;
    private Long eventId;
    private Long scheduleId;
    private Integer allowRegister; // 该轮次是否开放报名：1-开放，0-关闭
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
