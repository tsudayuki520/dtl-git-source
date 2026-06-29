package com.dlust.sportbackend.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TeamScoreAdjustment {
    private Long id;
    private Long teamId;
    private BigDecimal deltaAmount;
    private String note;
    private LocalDateTime createTime;
}
