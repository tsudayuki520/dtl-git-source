package com.dlust.sportbackend.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Result {
    private Long id;
    private Long sportsMeetingId;
    private Long eventId;
    private Long participantId;
    private BigDecimal score;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
