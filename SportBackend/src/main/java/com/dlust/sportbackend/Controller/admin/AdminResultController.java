package com.dlust.sportbackend.Controller.admin;

import com.dlust.sportbackend.Service.ResultService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/result")
public class AdminResultController {

    @Autowired
    private ResultService resultService;

    @GetMapping("/list")
    public Result<List<ResultVO>> list(@RequestParam Long sportsMeetingId) {
        log.info("查询成绩列表: sportsMeetingId={}", sportsMeetingId);
        return Result.success(resultService.getBySportsMeetingId(sportsMeetingId));
    }

    @GetMapping("/listByEvent")
    public Result<List<ResultVO>> listByEvent(@RequestParam Long eventId) {
        log.info("按项目查询成绩: eventId={}", eventId);
        return Result.success(resultService.getByEventId(eventId));
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody com.dlust.sportbackend.entity.Result result) {
        log.info("添加成绩: eventId={}, participantId={}, score={}", result.getEventId(), result.getParticipantId(), result.getScore());
        resultService.add(result);
        return Result.success("添加成功");
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody com.dlust.sportbackend.entity.Result result) {
        log.info("更新成绩: id={}", result.getId());
        resultService.update(result);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除成绩: id={}", id);
        resultService.delete(id);
        return Result.success("删除成功");
    }
}
