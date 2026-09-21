package com.dlust.sportbackend.Controller.user;

import com.dlust.sportbackend.Service.EventScheduleService;
import com.dlust.sportbackend.Service.EventService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/event")
public class UserEventController {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventScheduleService eventScheduleService;

    @GetMapping("/list")
    public Result<List<Event>> getList(@RequestParam(required = false) Long scheduleId,
                                        @RequestParam(required = false) Long sportsMeetingId,
                                        @RequestParam(required = false) Long groupTypeId) {
        log.info("获取项目列表: scheduleId={}, sportsMeetingId={}, groupTypeId={}", scheduleId, sportsMeetingId, groupTypeId);
        List<Event> events;
        if (scheduleId != null) {
            List<Long> eventIds = eventScheduleService.getEventIdsByScheduleId(scheduleId);
            if (eventIds.isEmpty()) {
                return Result.success(List.of());
            }
            // 一次批量查回，再按 eventIds 原顺序回填（IN 不保证顺序）
            Map<Long, Event> byId = new HashMap<>();
            for (Event e : eventService.getByIds(eventIds)) {
                byId.put(e.getId(), e);
            }
            events = new ArrayList<>();
            for (Long id : eventIds) {
                Event e = byId.get(id);
                if (e != null && (groupTypeId == null || groupTypeId.equals(e.getGroupTypeId()))) {
                    events.add(e);
                }
            }
        } else if (sportsMeetingId != null) {
            events = eventService.getBySportsMeetingId(sportsMeetingId);
            // 与 scheduleId 分支语义保持一致：传了 groupTypeId 就只返回面向该组别的项目
            if (groupTypeId != null) {
                events = events.stream()
                        .filter(e -> groupTypeId.equals(e.getGroupTypeId()))
                        .toList();
            }
        } else {
            return Result.error(400, "请传入 scheduleId 或 sportsMeetingId");
        }
        // 装配 currentOpenScheduleId（当前开放且 sort 最小的轮次），一次批量查
        List<Long> ids = events.stream().map(Event::getId).toList();
        Map<Long, Long> openScheduleIds = eventScheduleService.getOpenScheduleIdsByEventIds(ids);
        for (Event e : events) {
            e.setCurrentOpenScheduleId(openScheduleIds.get(e.getId()));
        }
        return Result.success(events);
    }

    @GetMapping("/{id}")
    public Result<Event> getById(@PathVariable Long id) {
        log.info("获取项目详情: id={}", id);
        Event event = eventService.getById(id);
        if (event == null) {
            return Result.error(404, "项目不存在");
        }
        event.setCurrentOpenScheduleId(eventScheduleService.getOpenScheduleIdByEventId(id));
        return Result.success(event);
    }
}
