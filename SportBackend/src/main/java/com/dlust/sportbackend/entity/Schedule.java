package com.dlust.sportbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Schedule {
    private Long id;
    private Long sportsMeetingId;
    private String name;        // 轮次名称：预选赛、复赛、决赛
    private Integer status;     // 0-进行中，1-已结束
    private Integer isDeleted;  // 0-正常，1-已删除
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
