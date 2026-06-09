package com.dlust.sportbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GroupType {
    private Long id;
    private Long sportsMeetingId;
    private String name;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
