package com.dlust.sportbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NoticeVO {
    private Long id;
    private String title;
    private String content;
    private Integer isRead;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
