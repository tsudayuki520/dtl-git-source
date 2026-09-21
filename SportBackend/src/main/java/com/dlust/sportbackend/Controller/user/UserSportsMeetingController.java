package com.dlust.sportbackend.Controller.user;

import com.dlust.sportbackend.Service.EventScheduleService;
import com.dlust.sportbackend.Service.EventService;
import com.dlust.sportbackend.Service.NoticeService;
import com.dlust.sportbackend.Service.RegistrationService;
import com.dlust.sportbackend.Service.SportsMeetingService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Event;
import com.dlust.sportbackend.entity.EventDetailVO;
import com.dlust.sportbackend.entity.SportsMeeting;
import com.dlust.sportbackend.entity.SportsMeetingDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/sports-meeting")
public class UserSportsMeetingController {

    @Autowired
    private SportsMeetingService sportsMeetingService;

    @Autowired
    private EventService eventService;

    @Autowired
    private EventScheduleService eventScheduleService;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private RegistrationService registrationService;

    @GetMapping("/list")
    public Result<List<SportsMeeting>> getList(@RequestParam(required = false) Integer status) {
        log.info("获取运动会列表: status={}", status);
        if (status != null) {
            return Result.success(sportsMeetingService.getByStatus(status));
        }
        // 小程序端不展示筹备中的运动会
        return Result.success(sportsMeetingService.getAll().stream()
                .filter(sm -> sm.getStatus() != null && sm.getStatus() != 0)
                .toList());
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

    /**
     * 详情页聚合：一次返回 meeting + 项目列表(含轮次名/已报名人数) + 赛事通知。
     * 原先小程序详情页 onShow 要发 6 个请求，这里合成 1 个。
     */
    @GetMapping("/{id}/detail")
    public Result<SportsMeetingDetailVO> getDetail(@PathVariable Long id) {
        log.info("获取运动会详情(聚合): id={}", id);
        SportsMeeting sm = sportsMeetingService.getById(id);
        if (sm == null) {
            return Result.error(404, "运动会不存在");
        }

        List<Event> events = eventService.getBySportsMeetingId(id);
        List<Long> eventIds = new ArrayList<>();
        for (Event e : events) {
            eventIds.add(e.getId());
        }
        Map<Long, Long> openScheduleIds = eventScheduleService.getOpenScheduleIdsByEventIds(eventIds);
        Map<Long, String> roundNames = eventScheduleService.getScheduleNameByEventIdForMeeting(id);
        Map<Long, Integer> counts = registrationService.countBySportsMeetingId(id);

        List<EventDetailVO> vos = new ArrayList<>();
        for (Event e : events) {
            EventDetailVO vo = new EventDetailVO();
            vo.setId(e.getId());
            vo.setName(e.getName());
            vo.setCategory(e.getCategory());
            vo.setGender(e.getGender());
            vo.setGroupTypeName(e.getGroupTypeName());
            vo.setAllowRegister(e.getAllowRegister());
            vo.setRegisterLimit(e.getRegisterLimit());
            vo.setCurrentOpenScheduleId(openScheduleIds.get(e.getId()));
            vo.setRoundName(roundNames.get(e.getId()));
            vo.setRegisteredCount(counts.getOrDefault(e.getId(), 0));
            vos.add(vo);
        }

        SportsMeetingDetailVO detail = new SportsMeetingDetailVO();
        detail.setMeeting(sm);
        detail.setEvents(vos);
        detail.setNotices(noticeService.getBySportsMeetingId(id));
        return Result.success(detail);
    }
}
