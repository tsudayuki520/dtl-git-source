package com.dlust.sportbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ResultVO {
    private Long id;
    private Long sportsMeetingId;
    private Long eventId;
    private Long eventScheduleId;
    private Long scheduleId;
    private Long participantId;
    // 成绩值：径赛存毫秒数，田赛存厘米数（按 category 区分）
    private Integer scoreValue;
    // 积分：用于代表队总分计算
    private Integer points;
    // 破纪录审核状态：0=未审，1=通过已入册，2=拒绝
    private Integer recordStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 关联查询字段
    private String participantName;
    private String eventName;
    private String scheduleName;
    // 项目分类：田赛/径赛/团队赛（决定 score_value 的单位与排序方向）
    private String category;
}
