package com.dlust.sportbackend.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Result {
    private Long id;
    private Long sportsMeetingId;
    private Long eventId;
    private Long eventScheduleId;
    // 非数据库字段：仅用于接收前端赛次参数，Service 内转换为 eventScheduleId
    private Long scheduleId;
    private Long participantId;
    private BigDecimal score;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
