package com.dlust.sportbackend.Controller;

import com.dlust.sportbackend.Service.SportsMeetingService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.SportsMeeting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sports-meeting")
public class SportsMeetingController {

    @Autowired
    private SportsMeetingService sportsMeetingService;

    @GetMapping("/list")
    public Result<List<SportsMeeting>> getList() {
        return Result.success(sportsMeetingService.getAll());
    }

    @GetMapping("/search")
    public Result<List<SportsMeeting>> search(@RequestParam String keyword) {
        return Result.success(sportsMeetingService.search(keyword));
    }

    @GetMapping("/{id}")
    public Result<SportsMeeting> getById(@PathVariable Long id) {
        SportsMeeting sm = sportsMeetingService.getById(id);
        if (sm == null) {
            return Result.error(404, "运动会不存在");
        }
        return Result.success(sm);
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody SportsMeeting sportsMeeting) {
        sportsMeetingService.add(sportsMeeting);
        return Result.success("添加成功");
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody SportsMeeting sportsMeeting) {
        sportsMeetingService.update(sportsMeeting);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        sportsMeetingService.delete(id);
        return Result.success("删除成功");
    }
}
