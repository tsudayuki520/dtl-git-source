package com.dlust.sportbackend.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Team {
    private Long id;
    private Long sportsMeetingId;
    private Long groupTypeId;
    private String name;
    private String leader;
    private String coach;
    private BigDecimal totalScore;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
