package com.dlust.sportbackend.Controller.user;

import com.dlust.sportbackend.Service.SportsMeetingService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.SportsMeeting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/sports-meeting")
public class UserSportsMeetingController {

    @Autowired
    private SportsMeetingService sportsMeetingService;

    @GetMapping("/list")
    public Result<List<SportsMeeting>> getList(@RequestParam(required = false) Integer status) {
        log.info("获取运动会列表: status={}", status);
        if (status != null) {
            return Result.success(sportsMeetingService.getByStatus(status));
        }
        return Result.success(sportsMeetingService.getAll());
    }

    @GetMapping("/search")
    public Result<List<SportsMeeting>> search(@RequestParam String keyword) {
        log.info("搜索运动会: keyword={}", keyword);
        return Result.success(sportsMeetingService.search(keyword));
    }

    @GetMapping("/{id}")
    public Result<SportsMeeting> getById(@PathVariable Long id) {
        log.info("获取运动会详情: id={}", id);
        SportsMeeting sm = sportsMeetingService.getById(id);
        if (sm == null) {
            return Result.error(404, "运动会不存在");
        }
        return Result.success(sm);
    }
}
