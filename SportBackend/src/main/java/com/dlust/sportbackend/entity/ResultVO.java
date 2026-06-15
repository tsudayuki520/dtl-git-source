package com.dlust.sportbackend.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ResultVO {
    private Long id;
    private Long sportsMeetingId;
    private Long eventId;
    private Long eventScheduleId;
    private Long scheduleId;
    private Long participantId;
    private BigDecimal score;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 关联查询字段
    private String participantName;
    private String eventName;
    private String scheduleName;
}
