package com.dlust.sportbackend.entity;

import lombok.Data;

import java.util.List;

/**
 * 小程序「运动会详情」聚合响应：一次请求返回原先 6 个接口的数据。
 */
@Data
public class SportsMeetingDetailVO {
    private SportsMeeting meeting;
    private List<EventDetailVO> events;
    private List<Notice> notices;
}
