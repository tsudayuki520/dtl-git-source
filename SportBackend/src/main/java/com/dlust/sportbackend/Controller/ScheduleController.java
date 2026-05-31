package com.dlust.sportbackend.Controller;

import com.dlust.sportbackend.Service.ScheduleService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Schedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @GetMapping("/list")
    public Result<List<Schedule>> getList(@RequestParam Long sportsMeetingId) {
        return Result.success(scheduleService.getBySportsMeetingId(sportsMeetingId));
    }

    @GetMapping("/{id}")
    public Result<Schedule> getById(@PathVariable Long id) {
        Schedule schedule = scheduleService.getById(id);
        if (schedule == null) {
            return Result.error(404, "赛程不存在");
        }
        return Result.success(schedule);
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody Schedule schedule) {
        scheduleService.add(schedule);
        return Result.success("添加成功");
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody Schedule schedule) {
        scheduleService.update(schedule);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return Result.success("删除成功");
    }
}
