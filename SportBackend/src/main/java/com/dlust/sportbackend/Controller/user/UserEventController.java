package com.dlust.sportbackend.Controller.user;

import com.dlust.sportbackend.Service.EventService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/event")
public class UserEventController {

    @Autowired
    private EventService eventService;

    @GetMapping("/list")
    public Result<List<Event>> getList(@RequestParam(required = false) Long scheduleId,
                                        @RequestParam(required = false) Long sportsMeetingId,
                                        @RequestParam(required = false) String groupType) {
        log.info("获取项目列表: scheduleId={}, sportsMeetingId={}, groupType={}", scheduleId, sportsMeetingId, groupType);
        if (scheduleId != null) {
            if (groupType != null && !groupType.isEmpty()) {
                return Result.success(eventService.getByScheduleIdAndGroupType(scheduleId, groupType));
            }
            return Result.success(eventService.getByScheduleId(scheduleId));
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
