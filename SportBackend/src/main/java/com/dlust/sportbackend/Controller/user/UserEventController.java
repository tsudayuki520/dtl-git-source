package com.dlust.sportbackend.Controller.user;

import com.dlust.sportbackend.Service.EventScheduleService;
import com.dlust.sportbackend.Service.EventService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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
        if (scheduleId != null) {
            List<Long> eventIds = eventScheduleService.getEventIdsByScheduleId(scheduleId);
            if (eventIds.isEmpty()) {
                return Result.success(List.of());
            }
            List<Event> events = eventIds.stream()
                    .map(eventService::getById)
                    .filter(e -> e != null)
                    .filter(e -> groupTypeId == null || groupTypeId.equals(e.getGroupTypeId()))
                    .collect(Collectors.toList());
            return Result.success(events);
        }
        if (sportsMeetingId != null) {
            return Result.success(eventService.getBySportsMeetingId(sportsMeetingId));
        }
        return Result.error(400, "请传入 scheduleId 或 sportsMeetingId");
    }

    @GetMapping("/{id}")
    public Result<Event> getById(@PathVariable Long id) {
        log.info("获取项目详情: id={}", id);
        Event event = eventService.getById(id);
        if (event == null) {
            return Result.error(404, "项目不存在");
        }
        return Result.success(event);
    }
}
