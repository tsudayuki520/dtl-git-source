package com.dlust.sportbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GroupType {
    private Long id;
    private Long sportsMeetingId;
    private String name;
    private Integer perPersonLimit;   // 每人限报项目数(0=不限)
    private String limitEventIds;     // 限报选中项目ID数组的JSON字符串(如"[1,2,3]"),DB为JSON列
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
