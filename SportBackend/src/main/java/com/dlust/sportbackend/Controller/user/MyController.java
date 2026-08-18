package com.dlust.sportbackend.Controller.user;

import com.dlust.sportbackend.Mapper.ParticipantMapper;
import com.dlust.sportbackend.Mapper.RegistrationMapper;
import com.dlust.sportbackend.Mapper.TeamMapper;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Participant;
import com.dlust.sportbackend.entity.RegistrationVO;
import com.dlust.sportbackend.entity.TeamVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 小程序「我的」tab：当前登录用户跨运动会的参赛项目聚合查询。
 * 路径 /api/my/** 由 WebMvcConfig 拦截，userId 由 AuthInterceptor 注入。
 */
@RestController
@RequestMapping("/api/my")
public class MyController {

    @Autowired
    private ParticipantMapper participantMapper;

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private TeamMapper teamMapper;

    @GetMapping("/registrations")
    public Result<List<Map<String, Object>>> myRegistrations(@RequestAttribute("userId") Long userId) {
        // 1. 查该 user 的所有 participant（跨运动会）
        List<Participant> participants = participantMapper.selectByUserId(userId);
        if (participants.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        // 2. 按 sports_meeting_id 分组（保持查询顺序：最新的运动会在前）
        Map<Long, List<Participant>> grouped = participants.stream()
                .collect(Collectors.groupingBy(Participant::getSportsMeetingId, LinkedHashMap::new, Collectors.toList()));
        // 3. 每组组装 {sportsMeetingId, sportsMeetingName, items:[{eventName,category,scheduleName}]}
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<Participant>> e : grouped.entrySet()) {
            List<Participant> ps = e.getValue();
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("sportsMeetingId", e.getKey());
            group.put("sportsMeetingName", ps.get(0).getSportsMeetingName());
            List<Map<String, Object>> items = new ArrayList<>();
            for (Participant p : ps) {
                List<RegistrationVO> regs = registrationMapper.selectVOByParticipantId(p.getId());
                for (RegistrationVO r : regs) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("eventName", r.getEventName());
                    item.put("category", r.getCategory());
                    item.put("scheduleName", r.getScheduleName());
                    items.add(item);
                }
            }
            group.put("items", items);
            result.add(group);
        }
        return Result.success(result);
    }

    @GetMapping("/teams")
    public Result<List<TeamVO>> myTeams(@RequestAttribute("userId") Long userId) {
        return Result.success(teamMapper.selectByUserId(userId));
    }
}
