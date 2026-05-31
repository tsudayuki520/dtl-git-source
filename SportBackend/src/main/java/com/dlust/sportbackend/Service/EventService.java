package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.Event;
import java.util.List;

public interface EventService {

    List<Event> getByScheduleId(Long scheduleId);

    List<Event> getByScheduleIdAndGroupType(Long scheduleId, String groupType);

    Event getById(Long id);

    void add(Event event);

    void update(Event event);

    void delete(Long id);
}
