package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.EventScheduleMapper;
import com.dlust.sportbackend.Mapper.ResultMapper;
import com.dlust.sportbackend.Service.ResultService;
import com.dlust.sportbackend.entity.EventSchedule;
import com.dlust.sportbackend.entity.Result;
import com.dlust.sportbackend.entity.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResultServiceImpl implements ResultService {

    @Autowired
    private ResultMapper resultMapper;

    @Autowired
    private EventScheduleMapper eventScheduleMapper;

    @Override
    public List<ResultVO> getBySportsMeetingId(Long sportsMeetingId) {
        return resultMapper.selectVOBySportsMeetingId(sportsMeetingId);
    }

    @Override
    public List<ResultVO> getByEventId(Long eventId) {
        return resultMapper.selectVOByEventId(eventId);
    }

    @Override
    public List<ResultVO> getByEventAndSchedule(Long eventId, Long scheduleId) {
        return resultMapper.selectVOByEventAndSchedule(eventId, scheduleId);
    }

    @Override
    public void add(Result result) {
        resolveEventScheduleId(result);
        resultMapper.insert(result);
    }

    @Override
    public void update(Result result) {
        resolveEventScheduleId(result);
        resultMapper.updateById(result);
    }

    @Override
    public void delete(Long id) {
        resultMapper.deleteById(id);
    }

    // 前端传 scheduleId（赛次），后端查出对应的 event_schedule.id 填入 eventScheduleId
    private void resolveEventScheduleId(Result result) {
        if (result.getEventScheduleId() == null && result.getScheduleId() != null) {
            EventSchedule es = eventScheduleMapper.selectByEventIdAndScheduleId(
                    result.getEventId(), result.getScheduleId());
            if (es == null) {
                throw new RuntimeException("该项目未配置该赛次");
            }
            result.setEventScheduleId(es.getId());
        }
    }
}
