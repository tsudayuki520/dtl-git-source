package com.dlust.sportbackend.Controller.user;

import com.dlust.sportbackend.Service.NoticeService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Notice;
import com.dlust.sportbackend.entity.NoticeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notice")
public class UserNoticeController {

    @Autowired
    private NoticeService noticeService;

    @GetMapping("/global")
    public Result<List<Notice>> getGlobal() {
        log.info("获取全局公告");
        return Result.success(noticeService.getGlobal());
    }

    @GetMapping("/sports-meeting")
    public Result<List<Notice>> getBySportsMeeting(@RequestParam Long sportsMeetingId) {
        log.info("获取赛事通知: sportsMeetingId={}", sportsMeetingId);
        return Result.success(noticeService.getBySportsMeetingId(sportsMeetingId));
    }

    @GetMapping("/list")
    public Result<List<NoticeVO>> getList(@RequestAttribute("userId") Long userId,
                                          @RequestParam(defaultValue = "false") Boolean onlyUnread) {
        log.info("获取通知列表: userId={}, onlyUnread={}", userId, onlyUnread);
        if (Boolean.TRUE.equals(onlyUnread)) {
            return Result.success(noticeService.getUnreadByUserId(userId));
        }
        return Result.success(noticeService.getByUserId(userId));
    }

    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount(@RequestAttribute("userId") Long userId) {
        log.info("获取未读通知数量: userId={}", userId);
        return Result.success(noticeService.countUnread(userId));
    }

    @PostMapping("/{id}/read")
    public Result<String> markAsRead(@PathVariable Long id, @RequestAttribute("userId") Long userId) {
        log.info("标记通知已读: id={}, userId={}", id, userId);
        noticeService.markAsRead(id, userId);
        return Result.success("已读");
    }

    @GetMapping("/{id}")
    public Result<Notice> getById(@PathVariable Long id) {
        log.info("获取通知详情: id={}", id);
        Notice notice = noticeService.getById(id);
        if (notice == null) {
            return Result.error(404, "通知不存在");
        }
        return Result.success(notice);
    }
}
