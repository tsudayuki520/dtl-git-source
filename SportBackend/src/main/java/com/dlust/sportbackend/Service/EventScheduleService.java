package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.EventSchedule;
import java.util.List;
import java.util.Map;

public interface EventScheduleService {

    List<EventSchedule> getByEventId(Long eventId);

    List<EventSchedule> getByScheduleId(Long scheduleId);

    List<Long> getScheduleIdsByEventId(Long eventId);

    List<Long> getEventIdsByScheduleId(Long scheduleId);

    List<EventSchedule> getBySportsMeetingId(Long sportsMeetingId);

    void saveEventSchedules(Long eventId, List<Long> scheduleIds);

    void deleteById(Long id);

    void deleteByScheduleId(Long scheduleId);

    void updateAllowRegister(Long eventId, Long scheduleId, Integer allowRegister);

    /** 取该 event 当前开放且 sort 最小的 scheduleId，无则 null */
    Long getOpenScheduleIdByEventId(Long eventId);

    /** 指定的 (eventId, scheduleId) 这一轮是否开放报名（轮次或关联被软删即为否） */
    boolean isScheduleOpen(Long eventId, Long scheduleId);

    /** 批量取「当前开放且 sort 最小」的 scheduleId，返回 eventId -> scheduleId（无开放轮次的 event 不在 map 中） */
    Map<Long, Long> getOpenScheduleIdsByEventIds(List<Long> eventIds);

    /** 某运动会下 eventId -> 轮次名称 */
    Map<Long, String> getScheduleNameByEventIdForMeeting(Long sportsMeetingId);
}
