package com.dlust.sportbackend.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Record {
    private Long id;
    private String groupType;
    private String eventName;
    private String unit;
    private String name;
    private BigDecimal score;
    private LocalDateTime recordTime;
    private String venue;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
