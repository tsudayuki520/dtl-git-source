package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.EventMapper;
import com.dlust.sportbackend.Service.EventService;
import com.dlust.sportbackend.entity.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EventServiceImpl implements EventService {

    @Autowired
    private EventMapper eventMapper;

    @Override
    public List<Event> getByScheduleId(Long scheduleId) {
        return eventMapper.selectByScheduleId(scheduleId);
    }

    @Override
    public List<Event> getBySportsMeetingId(Long sportsMeetingId) {
        return eventMapper.selectBySportsMeetingId(sportsMeetingId);
    }

    @Override
    public List<Event> getByScheduleIdAndGroupType(Long scheduleId, String groupType) {
        return eventMapper.selectByScheduleIdAndGroupType(scheduleId, groupType);
    }

    @Override
    public Event getById(Long id) {
        return eventMapper.selectById(id);
    }

    @Override
    public void add(Event event) {
        eventMapper.insert(event);
    }

    @Override
    public void update(Event event) {
        eventMapper.updateById(event);
    }

    @Override
    public void delete(Long id) {
        eventMapper.softDeleteById(id);
    }
}
