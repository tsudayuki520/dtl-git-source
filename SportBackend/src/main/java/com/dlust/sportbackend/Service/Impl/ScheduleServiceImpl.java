package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.ScheduleMapper;
import com.dlust.sportbackend.Service.ScheduleService;
import com.dlust.sportbackend.entity.Schedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Override
    public List<Schedule> getBySportsMeetingId(Long sportsMeetingId) {
        return scheduleMapper.selectBySportsMeetingId(sportsMeetingId);
    }

    @Override
    public Schedule getById(Long id) {
        return scheduleMapper.selectById(id);
    }

    @Override
    public void add(Schedule schedule) {
        scheduleMapper.insert(schedule);
    }

    @Override
    public void update(Schedule schedule) {
        scheduleMapper.updateById(schedule);
    }

    @Override
    public void delete(Long id) {
        scheduleMapper.softDeleteById(id);
    }
}
