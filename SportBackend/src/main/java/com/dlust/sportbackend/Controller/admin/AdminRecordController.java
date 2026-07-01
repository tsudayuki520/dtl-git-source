package com.dlust.sportbackend.Controller.admin;

import com.dlust.sportbackend.Service.RecordService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Record;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/record")
public class AdminRecordController {

    @Autowired
    private RecordService recordService;

    @GetMapping("/list")
    public Result<List<Record>> list(@RequestParam(required = false) Long sportsMeetingId,
                                     @RequestParam(required = false) String eventName,
                                     @RequestParam(required = false) String category) {
        log.info("查询记录: sportsMeetingId={}, eventName={}, category={}", sportsMeetingId, eventName, category);
        return Result.success(recordService.getAll(sportsMeetingId, eventName, category));
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody Record record) {
        log.info("添加记录: name={}, eventName={}", record.getName(), record.getEventName());
        recordService.add(record);
        return Result.success("添加成功");
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody Record record) {
        log.info("更新记录: id={}", record.getId());
        recordService.update(record);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除记录: id={}", id);
        recordService.delete(id);
        return Result.success("删除成功");
    }

    @PostMapping("/review")
    public Result<String> review(@RequestBody java.util.Map<String, Object> body) {
        Long resultId = body.get("resultId") == null ? null : Long.valueOf(body.get("resultId").toString());
        String action = body.get("action") == null ? null : body.get("action").toString();
        log.info("审核破纪录候选: resultId={}, action={}", resultId, action);
        recordService.reviewRecord(resultId, action);
        return Result.success("审核成功");
    }
}
