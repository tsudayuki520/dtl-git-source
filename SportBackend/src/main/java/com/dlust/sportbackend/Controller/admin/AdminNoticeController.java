package com.dlust.sportbackend.Controller.admin;

import com.dlust.sportbackend.Service.NoticeService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Notice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/notice")
public class AdminNoticeController {

    @Autowired
    private NoticeService noticeService;

    @PostMapping("/add")
    public Result<String> add(@RequestBody Notice notice) {
        log.info("添加通知: title={}", notice.getTitle());
        noticeService.add(notice);
        return Result.success("添加成功");
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody Notice notice) {
        log.info("更新通知: id={}", notice.getId());
        noticeService.update(notice);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除通知: id={}", id);
        noticeService.delete(id);
        return Result.success("删除成功");
    }
}
