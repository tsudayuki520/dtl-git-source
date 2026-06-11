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
}
