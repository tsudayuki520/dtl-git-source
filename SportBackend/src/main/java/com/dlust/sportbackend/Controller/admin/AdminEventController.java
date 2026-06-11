package com.dlust.sportbackend.Controller.admin;

import com.dlust.sportbackend.Service.EventService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/event")
public class AdminEventController {

    @Autowired
    private EventService eventService;

    @PostMapping("/add")
    public Result<Long> add(@RequestBody Event event) {
        log.info("添加项目: name={}", event.getName());
        eventService.add(event);
        return Result.success(event.getId());
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody Event event) {
        log.info("更新项目: id={}", event.getId());
        eventService.update(event);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除项目: id={}", id);
        eventService.delete(id);
        return Result.success("删除成功");
    }
}
