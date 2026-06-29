package com.dlust.sportbackend.Controller.admin;

import com.dlust.sportbackend.Service.EventScheduleService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.EventSchedule;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/event-schedule")
public class AdminEventScheduleController {

    @Autowired
    private EventScheduleService eventScheduleService;

    @GetMapping("/list")
    public Result<List<EventSchedule>> listByEvent(@RequestParam Long eventId) {
        log.info("获取项目的赛程关联: eventId={}", eventId);
        return Result.success(eventScheduleService.getByEventId(eventId));
    }

    @GetMapping("/listBySchedule")
    public Result<List<EventSchedule>> listBySchedule(@RequestParam Long scheduleId) {
        log.info("获取赛程的项目关联: scheduleId={}", scheduleId);
        return Result.success(eventScheduleService.getByScheduleId(scheduleId));
    }

    @GetMapping("/listBySportsMeeting")
    public Result<List<EventSchedule>> listBySportsMeeting(@RequestParam Long sportsMeetingId) {
        log.info("获取运动会的项目赛程关联: sportsMeetingId={}", sportsMeetingId);
        return Result.success(eventScheduleService.getBySportsMeetingId(sportsMeetingId));
    }

    @PostMapping("/save")
    public Result<String> save(@RequestBody EventScheduleSaveRequest request) {
        log.info("保存项目赛程关联: eventId={}, scheduleIds={}", request.getEventId(), request.getScheduleIds());
        eventScheduleService.saveEventSchedules(request.getEventId(), request.getScheduleIds());
        return Result.success("保存成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除项目赛程关联: id={}", id);
        eventScheduleService.deleteById(id);
        return Result.success("删除成功");
    }

    @PutMapping("/allow")
    public Result<String> toggleAllow(@RequestBody EventScheduleAllowRequest request) {
        log.info("切换轮次开放报名: eventId={}, scheduleId={}, allowRegister={}",
                request.getEventId(), request.getScheduleId(), request.getAllowRegister());
        eventScheduleService.updateAllowRegister(
                request.getEventId(), request.getScheduleId(), request.getAllowRegister());
        return Result.success("更新成功");
    }

    @Data
    public static class EventScheduleSaveRequest {
        private Long eventId;
        private List<Long> scheduleIds;
    }

    @Data
    public static class EventScheduleAllowRequest {
        private Long eventId;
        private Long scheduleId;
        private Integer allowRegister;
    }
}
