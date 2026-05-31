package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.Schedule;
import java.util.List;

public interface ScheduleService {

    List<Schedule> getBySportsMeetingId(Long sportsMeetingId);

    Schedule getById(Long id);

    void add(Schedule schedule);

    void update(Schedule schedule);

    void delete(Long id);
}
