package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.EventMapper;
import com.dlust.sportbackend.Mapper.EventScheduleMapper;
import com.dlust.sportbackend.Service.EventScheduleService;
import com.dlust.sportbackend.entity.Event;
import com.dlust.sportbackend.entity.EventSchedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EventScheduleServiceImpl implements EventScheduleService {

    @Autowired
    private EventScheduleMapper eventScheduleMapper;

    @Autowired
    private EventMapper eventMapper;

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
        // 1. 现有关联（已带 allow_register）
        List<EventSchedule> existing = eventScheduleMapper.selectByEventId(eventId);
        Map<Long, Integer> existingMap = new LinkedHashMap<>();
        for (EventSchedule es : existing) {
            existingMap.put(es.getScheduleId(), es.getAllowRegister() == null ? 0 : es.getAllowRegister());
        }

        // 2. 计算同步方案（selected 须按 sort 升序）
        List<Long> selectedSorted = (scheduleIds == null) ? List.of() : sortByScheduleSort(scheduleIds);
        boolean eventAllowRegister = isEventAllowRegister(eventId);
        Map<Long, Integer> plan = syncPlanForTest(existingMap, selectedSorted, eventAllowRegister);

        // 3. 删除：现有但不在 plan 里的
        for (Long sid : existingMap.keySet()) {
            if (!plan.containsKey(sid)) {
                eventScheduleMapper.deleteByScheduleIdForEvent(eventId, sid);
            }
        }

        // 4. 新增：plan 里 existing 没有的；保留的无需动作
        List<EventSchedule> toInsert = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : plan.entrySet()) {
            if (!existingMap.containsKey(e.getKey())) {
                EventSchedule es = new EventSchedule();
                es.setEventId(eventId);
                es.setScheduleId(e.getKey());
                es.setAllowRegister(e.getValue());
                toInsert.add(es);
            }
        }
        if (!toInsert.isEmpty()) {
            eventScheduleMapper.batchInsertWithAllow(toInsert);
        }
    }

    /** 按 schedule.sort 升序排列给定的 scheduleIds */
    private List<Long> sortByScheduleSort(List<Long> scheduleIds) {
        return eventScheduleMapper.selectScheduleIdsByEventIdOrdered(scheduleIds);
    }

    private boolean isEventAllowRegister(Long eventId) {
        Event e = eventMapper.selectById(eventId);
        return e != null && e.getAllowRegister() != null && e.getAllowRegister() == 1;
    }

    @Override
    public void deleteById(Long id) {
        eventScheduleMapper.deleteById(id);
    }

    @Override
    public void deleteByScheduleId(Long scheduleId) {
        eventScheduleMapper.deleteByScheduleId(scheduleId);
    }

    @Override
    public void updateAllowRegister(Long eventId, Long scheduleId, Integer allowRegister) {
        eventScheduleMapper.updateAllowRegister(eventId, scheduleId, allowRegister == null ? 0 : allowRegister);
    }

    @Override
    public Long getOpenScheduleIdByEventId(Long eventId) {
        return eventScheduleMapper.selectOpenScheduleIdByEventId(eventId);
    }

    /**
     * 计算关联同步方案（纯逻辑，便于单测）。
     * - existing 为空（首次创建）：总闸开时只把 selected 第一个（sort 最小）设 1，其余 0；总闸关全 0。
     * - existing 非空（编辑）：保留已有关联的 allow_register；新增的默认 0。
     * - existing 中不在 selected 里的 scheduleId 不出现在返回 Map（调用方据此删除）。
     */
    static Map<Long, Integer> syncPlanForTest(Map<Long, Integer> existing,
                                              List<Long> selectedSorted,
                                              boolean eventAllowRegister) {
        Map<Long, Integer> plan = new LinkedHashMap<>();
        boolean isNew = existing == null || existing.isEmpty();
        for (int i = 0; i < selectedSorted.size(); i++) {
            Long sid = selectedSorted.get(i);
            Integer cur = (existing == null) ? null : existing.get(sid);
            if (cur != null) {
                plan.put(sid, cur);
            } else if (isNew && eventAllowRegister && i == 0) {
                plan.put(sid, 1);
            } else {
                plan.put(sid, 0);
            }
        }
        return plan;
    }
}
