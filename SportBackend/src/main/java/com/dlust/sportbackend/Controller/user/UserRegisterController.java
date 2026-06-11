package com.dlust.sportbackend.Controller.user;

import com.dlust.sportbackend.Mapper.ParticipantMapper;
import com.dlust.sportbackend.Mapper.RegistrationMapper;
import com.dlust.sportbackend.Mapper.SportsMeetingMapper;
import com.dlust.sportbackend.Service.EventScheduleService;
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

    /**
     * 报名接口
     */
    @PostMapping("/submit")
    public Result<String> submit(@RequestBody Map<String, Object> body) {
        Long sportsMeetingId = Long.valueOf(body.get("sportsMeetingId").toString());
        Long eventId = Long.valueOf(body.get("eventId").toString());
        String userCode = body.get("userCode").toString();
        String name = body.get("name").toString();
        log.info("报名参赛: sportsMeetingId={}, eventId={}, userCode={}, name={}", sportsMeetingId, eventId, userCode, name);

        // 获取 scheduleId：前端传或取项目第一个赛次
        Long scheduleId = null;
        if (body.get("scheduleId") != null) {
            scheduleId = Long.valueOf(body.get("scheduleId").toString());
        } else {
            List<Long> scheduleIds = eventScheduleService.getScheduleIdsByEventId(eventId);
            if (scheduleIds != null && !scheduleIds.isEmpty()) {
                scheduleId = scheduleIds.get(0);
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
        String phone = body.get("phone").toString();
        String gender = body.get("gender").toString();
        String college = body.getOrDefault("college", "") != null ? body.get("college").toString() : "";
        String major = body.getOrDefault("major", "") != null ? body.get("major").toString() : "";

        // 1. 查找或创建参赛人员
        Participant participant = participantMapper.selectByUserCodeAndSportsMeetingId(userCode, sportsMeetingId);
        if (participant == null) {
            participant = new Participant();
            participant.setSportsMeetingId(sportsMeetingId);
            participant.setUserCode(userCode);
            participant.setName(name);
            participant.setPhone(phone);
            participant.setGender(gender);
            participant.setCollege(college);
            participant.setMajor(major);
            participantMapper.insert(participant);
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

        // 3. 插入报名记录
        Registration registration = new Registration();
        registration.setParticipantId(participant.getId());
        registration.setEventId(eventId);
        registration.setScheduleId(scheduleId);
        registration.setStatus(0);
        registrationMapper.insert(registration);

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
