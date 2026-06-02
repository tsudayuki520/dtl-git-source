package com.dlust.sportbackend.Controller.admin;

import com.dlust.sportbackend.Service.ScheduleService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Schedule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/schedule")
public class AdminScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @PostMapping("/add")
    public Result<String> add(@RequestBody Schedule schedule) {
        log.info("添加赛程: name={}", schedule.getName());
        scheduleService.add(schedule);
        return Result.success("添加成功");
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody Schedule schedule) {
        log.info("更新赛程: id={}", schedule.getId());
        scheduleService.update(schedule);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除赛程: id={}", id);
        scheduleService.delete(id);
        return Result.success("删除成功");
    }
}
