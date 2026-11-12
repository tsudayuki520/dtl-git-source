package com.dlust.sportbackend.Controller.user;

import com.dlust.sportbackend.Service.EventService;
import com.dlust.sportbackend.Service.ResultService;
import com.dlust.sportbackend.Service.SportsMeetingService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Event;
import com.dlust.sportbackend.entity.PublicResultVO;
import com.dlust.sportbackend.entity.SportsMeeting;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/result")
public class UserResultController {
    private final ResultService resultService;
    private final EventService eventService;
    private final SportsMeetingService sportsMeetingService;

    public UserResultController(ResultService resultService, EventService eventService,
                                SportsMeetingService sportsMeetingService) {
        this.resultService = resultService;
        this.eventService = eventService;
        this.sportsMeetingService = sportsMeetingService;
    }

    @GetMapping("/listByEvent")
    public Result<List<PublicResultVO>> listByEvent(@RequestParam Long eventId) {
        Event event = eventService.getById(eventId);
        if (event == null || Integer.valueOf(1).equals(event.getIsDeleted())) {
            return Result.error(404, "项目不存在");
        }
        SportsMeeting meeting = sportsMeetingService.getById(event.getSportsMeetingId());
        // 与管理端现行状态一致：2=进行中，3=已结束。
        if (meeting == null || (!Integer.valueOf(2).equals(meeting.getStatus())
                && !Integer.valueOf(3).equals(meeting.getStatus()))) {
            return Result.error(404, "暂无可查看的赛况");
        }
        return Result.success(resultService.getByEventId(eventId).stream()
                .map(PublicResultVO::from).toList());
    }
}
