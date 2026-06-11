package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.EventScheduleMapper;
import com.dlust.sportbackend.Service.EventScheduleService;
import com.dlust.sportbackend.entity.EventSchedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventScheduleServiceImpl implements EventScheduleService {

    @Autowired
    private EventScheduleMapper eventScheduleMapper;

    @Override
    public List<EventSchedule> getByEventId(Long eventId) {
        return eventScheduleMapper.selectByEventId(eventId);
    }

    @Override
    public List<EventSchedule> getByScheduleId(Long scheduleId) {
        return eventScheduleMapper.selectByScheduleId(scheduleId);
    }

    @Override
    public List<Long> getScheduleIdsByEventId(Long eventId) {
        return eventScheduleMapper.selectScheduleIdsByEventId(eventId);
    }

    @Override
    public List<Long> getEventIdsByScheduleId(Long scheduleId) {
        return eventScheduleMapper.selectEventIdsByScheduleId(scheduleId);
    }

    @Override
    public List<EventSchedule> getBySportsMeetingId(Long sportsMeetingId) {
        return eventScheduleMapper.selectBySportsMeetingId(sportsMeetingId);
    }

    @Override
    @Transactional
    public void saveEventSchedules(Long eventId, List<Long> scheduleIds) {
        eventScheduleMapper.deleteByEventId(eventId);
        if (scheduleIds != null && !scheduleIds.isEmpty()) {
            eventScheduleMapper.batchInsert(eventId, scheduleIds);
        }
    }

    @Override
    public void deleteById(Long id) {
        eventScheduleMapper.deleteById(id);
    }

    @Override
    public void deleteByScheduleId(Long scheduleId) {
        eventScheduleMapper.deleteByScheduleId(scheduleId);
    }
}
