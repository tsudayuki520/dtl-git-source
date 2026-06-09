package com.dlust.sportbackend.Controller.admin;

import com.dlust.sportbackend.Service.SportsMeetingService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.SportsMeeting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/sports-meeting")
public class AdminSportsMeetingController {

    @Autowired
    private SportsMeetingService sportsMeetingService;

    @GetMapping("/list")
    public Result<java.util.List<SportsMeeting>> list() {
        log.info("管理端查询运动会列表（全部）");
        return Result.success(sportsMeetingService.getAll());
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody SportsMeeting sportsMeeting) {
        log.info("添加运动会: name={}", sportsMeeting.getName());
        sportsMeetingService.add(sportsMeeting);
        return Result.success("添加成功");
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody SportsMeeting sportsMeeting) {
        log.info("更新运动会: id={}", sportsMeeting.getId());
        sportsMeetingService.update(sportsMeeting);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除运动会: id={}", id);
        sportsMeetingService.delete(id);
        return Result.success("删除成功");
    }
}
