package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.Event;
import java.util.List;

public interface EventService {

    List<Event> getBySportsMeetingId(Long sportsMeetingId);

    Event getById(Long id);

    void add(Event event);

    void update(Event event);

    void delete(Long id);
}
