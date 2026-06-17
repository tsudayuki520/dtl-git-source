package com.dlust.sportbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Result {
    private Long id;
    private Long sportsMeetingId;
    private Long eventId;
    private Long eventScheduleId;
    // 非数据库字段：仅用于接收前端赛次参数，Service 内转换为 eventScheduleId
    private Long scheduleId;
    private Long participantId;
    // 成绩值：径赛存毫秒数，田赛存厘米数（按 event.category 区分）
    private Integer scoreValue;
    // 积分：用于代表队总分计算
    private Integer points;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
