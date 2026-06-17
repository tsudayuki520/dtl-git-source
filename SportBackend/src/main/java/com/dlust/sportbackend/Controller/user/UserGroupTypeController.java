package com.dlust.sportbackend.Controller.user;

import com.dlust.sportbackend.Service.GroupTypeService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.GroupType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/group-type")
public class UserGroupTypeController {

    @Autowired
    private GroupTypeService groupTypeService;

    @GetMapping("/list")
    public Result<List<GroupType>> list(@RequestParam Long sportsMeetingId) {
        log.info("获取组别列表: sportsMeetingId={}", sportsMeetingId);
        return Result.success(groupTypeService.getBySportsMeetingId(sportsMeetingId));
    }
}
