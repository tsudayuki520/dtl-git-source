package com.dlust.sportbackend.Controller.user;

import com.dlust.sportbackend.Mapper.ParticipantMapper;
import com.dlust.sportbackend.Mapper.RegistrationMapper;
import com.dlust.sportbackend.Mapper.SportsMeetingMapper;
import com.dlust.sportbackend.Service.EventScheduleService;
import com.dlust.sportbackend.Service.RegistrationService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Participant;
import com.dlust.sportbackend.entity.Registration;
import com.dlust.sportbackend.entity.SportsMeeting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/register")
public class UserRegisterController {

    @Autowired
    private ParticipantMapper participantMapper;

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private SportsMeetingMapper sportsMeetingMapper;

    @Autowired
    private EventScheduleService eventScheduleService;

    @Autowired
    private RegistrationService registrationService;

    /**
     * 报名接口
     * 身份来自 JWT 注入的 userId；不再接受前端传入的 userCode/name/phone 等身份字段。
     * 参赛人员按 (userId, sportsMeetingId) 查找，不存在则自动创建（upsert）。
     */
    @PostMapping("/submit")
    public Result<String> submit(@RequestAttribute("userId") Long userId,
                                 @RequestBody Map<String, Object> body) {
        Long sportsMeetingId = ((Number) body.get("sportsMeetingId")).longValue();
        Long eventId = ((Number) body.get("eventId")).longValue();
        log.info("报名参赛: userId={}, sportsMeetingId={}, eventId={}", userId, sportsMeetingId, eventId);

        // 获取 scheduleId：前端传，或取该项目「当前开放且 sort 最小」的轮次
        Long scheduleId = null;
        if (body.get("scheduleId") != null) {
            scheduleId = ((Number) body.get("scheduleId")).longValue();
        } else {
            scheduleId = eventScheduleService.getOpenScheduleIdByEventId(eventId);
            if (scheduleId == null) {
                return Result.error(400, "该项目当前未开放报名");
            }
        }

        // 0. 校验报名时间
        SportsMeeting meeting = sportsMeetingMapper.selectById(sportsMeetingId);
        if (meeting == null) {
            return Result.error(404, "运动会不存在");
        }
        if (meeting.getStatus() != 1) {
            return Result.error(400, "当前不在报名时间内");
        }
        LocalDateTime now = LocalDateTime.now();
        if (meeting.getRegistrationStart() != null && now.isBefore(meeting.getRegistrationStart())) {
            return Result.error(400, "报名尚未开始");
        }
        if (meeting.getRegistrationEnd() != null && now.isAfter(meeting.getRegistrationEnd())) {
            return Result.error(400, "报名已截止");
        }

        // 1. upsert participant(userId + sports_meeting_id)
        Participant participant = participantMapper.selectByUserIdAndSportsMeetingId(userId, sportsMeetingId);
        if (participant == null) {
            participant = new Participant();
            participant.setSportsMeetingId(sportsMeetingId);
            participant.setUserId(userId);
            participantMapper.insert(participant); // useGeneratedKeys=true → id 回填
        }

        // 2. 检查是否已报名该项目该赛次
        if (scheduleId != null) {
            Registration existing = registrationMapper.selectByParticipantIdEventIdScheduleId(participant.getId(), eventId, scheduleId);
            if (existing != null) {
                return Result.error(400, "您已报名该项目");
            }
        } else {
            Registration existing = registrationMapper.selectByParticipantIdAndEventId(participant.getId(), eventId);
            if (existing != null) {
                return Result.error(400, "您已报名该项目");
            }
        }

        // 3. 插入报名记录（含每人限报校验）
        try {
            registrationService.add(participant, eventId, scheduleId);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }

        return Result.success("报名成功");
    }

    /**
     * 获取某运动会下各项目的已报名人数
     */
    @GetMapping("/count")
    public Result<Map<Long, Integer>> getCount(@RequestParam Long sportsMeetingId) {
        log.info("获取报名人数统计: sportsMeetingId={}", sportsMeetingId);
        List<Map<String, Object>> list = registrationMapper.countBySportsMeetingId(sportsMeetingId);
        Map<Long, Integer> countMap = new HashMap<>();
        for (Map<String, Object> row : list) {
            Long eventId = Long.valueOf(row.get("eventId").toString());
            Integer count = Integer.valueOf(row.get("count").toString());
            countMap.put(eventId, count);
        }
        return Result.success(countMap);
    }
}
