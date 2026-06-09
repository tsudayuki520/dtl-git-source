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
    public Result<List<Record>> list() {
        log.info("查询全部记录");
        return Result.success(recordService.getAll());
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
}
