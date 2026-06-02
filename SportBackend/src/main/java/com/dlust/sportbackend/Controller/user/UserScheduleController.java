package com.dlust.sportbackend.Controller.user;

import com.dlust.sportbackend.Service.ScheduleService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Schedule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/schedule")
public class UserScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @GetMapping("/list")
    public Result<List<Schedule>> getList(@RequestParam Long sportsMeetingId) {
        log.info("获取赛程列表: sportsMeetingId={}", sportsMeetingId);
        return Result.success(scheduleService.getBySportsMeetingId(sportsMeetingId));
    }

    @GetMapping("/{id}")
    public Result<Schedule> getById(@PathVariable Long id) {
        log.info("获取赛程详情: id={}", id);
        Schedule schedule = scheduleService.getById(id);
        if (schedule == null) {
            return Result.error(404, "赛程不存在");
        }
        return Result.success(schedule);
    }
}
