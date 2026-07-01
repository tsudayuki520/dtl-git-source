package com.dlust.sportbackend.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Record {
    private Long id;
    private Long sportsMeetingId;
    private String groupType;
    private String eventName;
    private String category;
    private String unit;
    private String name;
    private BigDecimal score;
    private Integer scoreValue;
    private Long resultId;
    private LocalDateTime recordTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
