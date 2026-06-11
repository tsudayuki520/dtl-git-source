package com.dlust.sportbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Registration {
    private Long id;
    private Long participantId;
    private Long eventId;
    private Long scheduleId;
    private Integer status;  // 0-已报名，1-已晋级，2-已取消
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
