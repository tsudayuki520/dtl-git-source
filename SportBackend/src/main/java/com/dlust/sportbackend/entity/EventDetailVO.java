package com.dlust.sportbackend.entity;

import lombok.Data;

/**
 * 运动会详情页用的事件条目：只带小程序端展示所需字段 + 轮次名 + 已报名人数。
 */
@Data
public class EventDetailVO {
    private Long id;
    private String name;
    private String category;      // 田赛、径赛、团队赛
    private String gender;        // 男、女、不限
    private String groupTypeName; // 组别名称
    private Integer allowRegister;    // 1-支持报名，0-不支持（仅晋级）
    private Integer registerLimit;    // 报名上限，0表示不限
    private Long currentOpenScheduleId; // 当前开放且 sort 最小的轮次ID，null 表示无开放轮次
    private String roundName;         // 所属轮次名称（预赛/复赛/决赛）
    private Integer registeredCount;  // 已报名人数(status=0)
}
