package com.dlust.sportbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GroupType {
    private Long id;
    private Long sportsMeetingId;
    private String name;
    private Integer perTeamLimit;       // 每代表队在选定项目中限报人数(0=不限)
    private String limitEventIds;       // 每代表队限报选中项目ID数组JSON(如"[1,2,3]")
    private Integer perPersonLimit;     // 每人限报项目数(0=不限)
    private String personLimitEventIds; // 每人限报选中项目ID数组JSON
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
