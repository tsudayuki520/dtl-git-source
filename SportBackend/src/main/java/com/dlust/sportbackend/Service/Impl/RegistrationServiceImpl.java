package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.GroupTypeMapper;
import com.dlust.sportbackend.Mapper.ParticipantMapper;
import com.dlust.sportbackend.Mapper.RegistrationMapper;
import com.dlust.sportbackend.Mapper.TeamMapper;
import com.dlust.sportbackend.Service.EventScheduleService;
import com.dlust.sportbackend.Service.RegistrationService;
import com.dlust.sportbackend.entity.GroupType;
import com.dlust.sportbackend.entity.Participant;
import com.dlust.sportbackend.entity.Registration;
import com.dlust.sportbackend.entity.RegistrationVO;
import com.dlust.sportbackend.entity.Team;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class RegistrationServiceImpl implements RegistrationService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private EventScheduleService eventScheduleService;

    @Autowired
    private ParticipantMapper participantMapper;

    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private GroupTypeMapper groupTypeMapper;

    @Override
    public List<RegistrationVO> getBySportsMeetingId(Long sportsMeetingId) {
        return registrationMapper.selectVOBySportsMeetingId(sportsMeetingId);
    }

    @Override
    public List<RegistrationVO> getByEventId(Long eventId) {
        return registrationMapper.selectVOByEventId(eventId);
    }

    @Override
    public void add(Long participantId, Long eventId, Long scheduleId) {
        checkRegisterLimit(participantId, eventId);
        Registration reg = new Registration();
        reg.setParticipantId(participantId);
        reg.setEventId(eventId);
        reg.setScheduleId(scheduleId);
        reg.setStatus(0);
        registrationMapper.insert(reg);
    }

    /**
     * 每人限报校验:participant → team → group_type,取组别限报规则;
     * 仅当 N>0 且选中项目集非空 且 本次 event 在选中集时才计数。
     * 计数=该人在选中集内已报的不同event数(status∈{0,1}),同event多赛次算1。
     */
    private void checkRegisterLimit(Long participantId, Long eventId) {
        Participant p = participantMapper.selectById(participantId);
        if (p == null || p.getTeamId() == null) return;
        Team team = teamMapper.selectById(p.getTeamId());
        if (team == null || team.getGroupTypeId() == null) return;
        GroupType gt = groupTypeMapper.selectById(team.getGroupTypeId());
        if (gt == null) return;

        Integer n = gt.getPerPersonLimit();
        if (n == null || n <= 0) return;

        List<Long> limitEventIds = parseLongList(gt.getLimitEventIds());
        if (limitEventIds == null || limitEventIds.isEmpty()) return;
        if (!limitEventIds.contains(eventId)) return;  // 本次项目不在限制范围

        // 已报的不同event数(本次尚未insert,故不含本次)
        int counted = registrationMapper.countDistinctEventByParticipantInEvents(
                participantId, limitEventIds, Arrays.asList(0, 1));
        // 判断本次event是否已有active报名(复用计数查询,排除已取消status=2)
        boolean alreadyActive = registrationMapper.countDistinctEventByParticipantInEvents(
                participantId, Arrays.asList(eventId), Arrays.asList(0, 1)) > 0;
        int after = alreadyActive ? counted : counted + 1;
        if (after > n) {
            throw new RuntimeException("超出限报:每人最多报 " + n + " 个项目");
        }
    }

    private List<Long> parseLongList(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.warn("解析limitEventIds失败: {}", json, e);
            return null;
        }
    }

    @Override
    public void update(Long id, Integer status) {
        Registration reg = registrationMapper.selectById(id);
        Integer oldStatus = (reg != null) ? reg.getStatus() : null;

        registrationMapper.updateStatus(id, status);

        if (status == 1) {
            autoRegisterNextSchedule(id);
        }

        if (oldStatus != null && oldStatus == 1 && status != 1) {
            removeNextScheduleRegistration(reg);
        }
    }

    private void autoRegisterNextSchedule(Long registrationId) {
        Registration reg = registrationMapper.selectById(registrationId);
        if (reg == null || reg.getEventId() == null || reg.getScheduleId() == null) return;

        List<Long> scheduleIds = eventScheduleService.getScheduleIdsByEventId(reg.getEventId());
        if (scheduleIds == null || scheduleIds.isEmpty()) return;

        int currentIndex = scheduleIds.indexOf(reg.getScheduleId());
        if (currentIndex < 0 || currentIndex >= scheduleIds.size() - 1) return;

        Long nextScheduleId = scheduleIds.get(currentIndex + 1);

        Registration existing = registrationMapper.selectByParticipantIdEventIdScheduleId(
                reg.getParticipantId(), reg.getEventId(), nextScheduleId);
        if (existing != null) return;

        Registration newReg = new Registration();
        newReg.setParticipantId(reg.getParticipantId());
        newReg.setEventId(reg.getEventId());
        newReg.setScheduleId(nextScheduleId);
        newReg.setStatus(0);
        registrationMapper.insert(newReg);
        log.info("晋级自动报名: participantId={}, eventId={}, nextScheduleId={}",
                reg.getParticipantId(), reg.getEventId(), nextScheduleId);
    }

    private void removeNextScheduleRegistration(Registration reg) {
        if (reg == null || reg.getEventId() == null || reg.getScheduleId() == null) return;

        List<Long> scheduleIds = eventScheduleService.getScheduleIdsByEventId(reg.getEventId());
        if (scheduleIds == null || scheduleIds.isEmpty()) return;

        int currentIndex = scheduleIds.indexOf(reg.getScheduleId());
        if (currentIndex < 0 || currentIndex >= scheduleIds.size() - 1) return;

        Long nextScheduleId = scheduleIds.get(currentIndex + 1);

        Registration existing = registrationMapper.selectByParticipantIdEventIdScheduleId(
                reg.getParticipantId(), reg.getEventId(), nextScheduleId);
        if (existing != null) {
            registrationMapper.deleteById(existing.getId());
            log.info("取消晋级，删除下一赛次报名: participantId={}, eventId={}, nextScheduleId={}",
                    reg.getParticipantId(), reg.getEventId(), nextScheduleId);
        }
    }

    @Override
    public void delete(Long id) {
        registrationMapper.deleteById(id);
    }
}
