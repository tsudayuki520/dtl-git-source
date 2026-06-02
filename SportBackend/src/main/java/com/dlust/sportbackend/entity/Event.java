package com.dlust.sportbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Event {
    private Long id;
    private Long sportsMeetingId;
    private Long scheduleId;
    private String name;        // 项目名称：100米短跑、跳远等
    private String category;    // 项目分类：田赛、径赛、趣味赛
    private String gender;      // 性别限制：男、女、不限
    private String groupType;   // 组别：学生组、教工组
    private Integer status;     // 0-进行中，1-已结束
    private Integer allowRegister; // 1-支持报名，0-不支持（仅晋级）
    private Integer registerLimit; // 报名上限，0表示不限
    private Integer isDeleted;  // 0-正常，1-已删除
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
