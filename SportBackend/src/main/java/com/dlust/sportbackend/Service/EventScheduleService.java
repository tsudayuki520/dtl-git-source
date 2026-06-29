package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.EventSchedule;
import java.util.List;

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
}
