package com.dlust.sportbackend.Controller;

import com.dlust.sportbackend.Service.EventService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/event")
public class EventController {

    @Autowired
    private EventService eventService;

    @GetMapping("/list")
    public Result<List<Event>> getList(@RequestParam Long scheduleId,
                                        @RequestParam(required = false) String groupType) {
        if (groupType != null && !groupType.isEmpty()) {
            return Result.success(eventService.getByScheduleIdAndGroupType(scheduleId, groupType));
        }
        return Result.success(eventService.getByScheduleId(scheduleId));
    }

    @GetMapping("/{id}")
    public Result<Event> getById(@PathVariable Long id) {
        Event event = eventService.getById(id);
        if (event == null) {
            return Result.error(404, "项目不存在");
        }
        return Result.success(event);
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody Event event) {
        eventService.add(event);
        return Result.success("添加成功");
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody Event event) {
        eventService.update(event);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        eventService.delete(id);
        return Result.success("删除成功");
    }
}
