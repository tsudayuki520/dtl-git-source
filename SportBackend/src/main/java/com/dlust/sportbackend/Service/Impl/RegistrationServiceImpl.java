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
    public List<RegistrationVO> getByParticipantId(Long participantId) {
        return registrationMapper.selectVOByParticipantId(participantId);
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
     * 报名校验(三步):
     * ① 资格:参赛人员必须有代表队(participant→team→group_type),任一缺失即拒绝;
     * ② 规则A:每代表队在「代表队选中集(limit_event_ids)」项目上限报人数(per_team_limit),
     *          统计该队在该 event 已报不同参赛人数(status∈{0,1}),达 M 即拒绝;
     * ③ 规则B:每人限报项目数(per_person_limit),统计「每人选中集(person_limit_event_ids)」内
     *          已报不同 event 数(只算 status=0,忽略已晋级),含本次后超过 N 即拒绝。
     */
    private void checkRegisterLimit(Long participantId, Long eventId) {
        Participant p = participantMapper.selectById(participantId);
        if (p == null) return;

        // ① 资格:参赛人员必须有代表队(participant→team→group_type)
        if (p.getTeamId() == null) {
            throw new RuntimeException("请先加入代表队后再报名");
        }
        Team team = teamMapper.selectById(p.getTeamId());
        if (team == null || team.getGroupTypeId() == null) {
            throw new RuntimeException("请先加入代表队后再报名");
        }
        GroupType gt = groupTypeMapper.selectById(team.getGroupTypeId());
        if (gt == null) {
            throw new RuntimeException("请先加入代表队后再报名");
        }

        // ② 规则A:每代表队在「代表队选中集」项目上限报人数
        Integer m = gt.getPerTeamLimit();
        List<Long> teamEventIds = parseLongList(gt.getLimitEventIds());
        if (m != null && m > 0 && teamEventIds != null && !teamEventIds.isEmpty()
                && teamEventIds.contains(eventId)) {
            int counted = registrationMapper.countDistinctParticipantByTeamAndEvent(
                    p.getTeamId(), eventId, Arrays.asList(0, 1));
            if (counted >= m) {
                throw new RuntimeException("超出限报:该代表队在此项目最多报 " + m + " 人");
            }
        }

        // ③ 规则B:每人限报项目数(每人选中集内,只统计 status=0 的已报名)
        Integer n = gt.getPerPersonLimit();
        List<Long> personEventIds = parseLongList(gt.getPersonLimitEventIds());
        if (n != null && n > 0 && personEventIds != null && !personEventIds.isEmpty()
                && personEventIds.contains(eventId)) {
            int counted = registrationMapper.countDistinctEventByParticipantInEvents(
                    participantId, personEventIds, Arrays.asList(0));
            boolean alreadyActive = registrationMapper.countDistinctEventByParticipantInEvents(
                    participantId, Arrays.asList(eventId), Arrays.asList(0)) > 0;
            int after = alreadyActive ? counted : counted + 1;
            if (after > n) {
                throw new RuntimeException("超出限报:每人最多报 " + n + " 个项目");
            }
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
