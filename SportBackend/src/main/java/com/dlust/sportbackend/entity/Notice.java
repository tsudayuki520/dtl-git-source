package com.dlust.sportbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Notice {
    private Long id;
    private Long sportsMeetingId;  // null 表示全局公告
    private String title;
    private String content;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
