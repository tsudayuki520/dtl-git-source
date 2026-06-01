package com.dlust.sportbackend.Controller;

import com.dlust.sportbackend.Service.NoticeService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Notice;
import com.dlust.sportbackend.entity.NoticeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    /**
     * 获取全局公告（首页用，无需登录）
     */
    @GetMapping("/global")
    public Result<List<Notice>> getGlobal() {
        return Result.success(noticeService.getGlobal());
    }

    /**
     * 获取某运动会的赛事通知（公开接口）
     */
    @GetMapping("/sports-meeting")
    public Result<List<Notice>> getBySportsMeeting(@RequestParam Long sportsMeetingId) {
        return Result.success(noticeService.getBySportsMeetingId(sportsMeetingId));
    }

    /**
     * 获取通知列表（带已读状态）
     * userId 从 token 中获取
     */
    @GetMapping("/list")
    public Result<List<NoticeVO>> getList(@RequestAttribute("userId") Long userId,
                                          @RequestParam(defaultValue = "false") Boolean onlyUnread) {
        if (Boolean.TRUE.equals(onlyUnread)) {
            return Result.success(noticeService.getUnreadByUserId(userId));
        }
        return Result.success(noticeService.getByUserId(userId));
    }

    /**
     * 获取未读数量
     */
    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount(@RequestAttribute("userId") Long userId) {
        return Result.success(noticeService.countUnread(userId));
    }

    /**
     * 标记已读
     */
    @PostMapping("/{id}/read")
    public Result<String> markAsRead(@PathVariable Long id, @RequestAttribute("userId") Long userId) {
        noticeService.markAsRead(id, userId);
        return Result.success("已读");
    }

    @GetMapping("/{id}")
    public Result<Notice> getById(@PathVariable Long id) {
        Notice notice = noticeService.getById(id);
        if (notice == null) {
            return Result.error(404, "通知不存在");
        }
        return Result.success(notice);
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody Notice notice) {
        noticeService.add(notice);
        return Result.success("添加成功");
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody Notice notice) {
        noticeService.update(notice);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.success("删除成功");
    }
}
