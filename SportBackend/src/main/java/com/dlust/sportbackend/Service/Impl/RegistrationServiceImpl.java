package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.EventMapper;
import com.dlust.sportbackend.Mapper.GroupTypeMapper;
import com.dlust.sportbackend.Mapper.ParticipantMapper;
import com.dlust.sportbackend.Mapper.RegistrationMapper;
import com.dlust.sportbackend.Mapper.TeamMapper;
import com.dlust.sportbackend.Service.EventScheduleService;
import com.dlust.sportbackend.Service.RegistrationService;
import com.dlust.sportbackend.Service.ResultService;
import com.dlust.sportbackend.entity.Event;
import com.dlust.sportbackend.entity.GroupType;
import com.dlust.sportbackend.entity.Participant;
import com.dlust.sportbackend.entity.Registration;
import com.dlust.sportbackend.entity.RegistrationVO;
import com.dlust.sportbackend.entity.ResultVO;
import com.dlust.sportbackend.entity.Team;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RegistrationServiceImpl implements RegistrationService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private ParticipantMapper participantMapper;

    @Autowired
    private EventScheduleService eventScheduleService;

    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private GroupTypeMapper groupTypeMapper;

    @Autowired
    private EventMapper eventMapper;

    @Autowired
    private ResultService resultService;

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
    public Map<Long, Integer> countBySportsMeetingId(Long sportsMeetingId) {
        List<Map<String, Object>> list = registrationMapper.countBySportsMeetingId(sportsMeetingId);
        Map<Long, Integer> countMap = new HashMap<>();
        for (Map<String, Object> row : list) {
            Long eventId = Long.valueOf(row.get("eventId").toString());
            Integer count = Integer.valueOf(row.get("count").toString());
            countMap.put(eventId, count);
        }
        return countMap;
    }

    @Override
    public void add(Long participantId, Long eventId, Long scheduleId) {
        // 管理端入口：管理员可指定任意人参加任意项目，故跳过「组别匹配」与「轮次必须开放」校验，
        // 其余（项目有效、代表队资格、限报规则）照常执行
        Participant p = participantMapper.selectByIdWithUser(participantId);
        if (p == null) throw new RuntimeException("参赛人员不存在");
        Long resolvedScheduleId = checkRegisterLimit(p, eventId, scheduleId, false);
        insertRegistration(p.getId(), eventId, resolvedScheduleId);
    }

    @Override
    public void add(Participant participant, Long eventId, Long scheduleId) {
        // 小程序端入口：含「项目必须面向报名人所在组别」校验
        Long resolvedScheduleId = checkRegisterLimit(participant, eventId, scheduleId, true);
        // 校验全部通过后才落库，避免报名失败时留下无代表队的孤儿 participant
        if (participant.getId() == null) {
            participantMapper.insert(participant);
        }
        insertRegistration(participant.getId(), eventId, resolvedScheduleId);
    }

    private void insertRegistration(Long participantId, Long eventId, Long scheduleId) {
        Registration reg = new Registration();
        reg.setParticipantId(participantId);
        reg.setEventId(eventId);
        reg.setScheduleId(scheduleId);
        reg.setStatus(0);
        registrationMapper.insert(reg);
    }

    /**
     * 报名校验，按「便宜→贵、静态→动态、无副作用→有副作用」排序：
     * ① 项目静态校验：存在且未软删、属于同一运动会、未结束；
     * ② 资格链(participant→team→group_type)：任一级缺失则「组别/代表队维度」的规则(③⑥⑦)整体跳过，
     *           不拦截报名 —— 没有组别和代表队的人只受项目自身规则(⑧)约束；
     * ③ 组别匹配(仅小程序端)：项目必须面向报名人所在组别；管理端可指定任意人参加任意项目，跳过本条；
     * ④ 轮次：小程序端显式传入的轮次也必须确实开放，未传则由后端取「开放且 sort 最小」的一轮；
     *           管理端保持原样（可传任意轮次，也可传 null）；
     * ⑤ 是否已报名该项目该赛次；
     * ⑥ 规则A：每代表队在「代表队选中集(limit_event_ids)」项目上限报人数(per_team_limit)，
     *           统计该队在该 event 已报不同参赛人数(status∈{0,1})，达 M 即拒绝；
     * ⑦ 规则B：每人限报项目数(per_person_limit)，统计「每人选中集(person_limit_event_ids)」内
     *           已报不同 event 数(只算 status=0，忽略已晋级)，含本次后超过 N 即拒绝；
     * ⑧ 规则C：项目自身报名上限(event.register_limit)，统计该项目已报人数(status=0)，
     *           达上限即拒绝，0 表示不限。统计口径与前端「已报名 X / N」一致(countByEventId)。
     *
     * @param strict 小程序端为 true（多做③组别匹配与④轮次开放校验）；管理端为 false
     * @return 最终要报名的 scheduleId
     */
    private Long checkRegisterLimit(Participant p, Long eventId, Long scheduleId, boolean strict) {
        if (p == null) throw new RuntimeException("参赛人员不存在");
        Long participantId = p.getId();

        // ① 项目静态校验（两种入口都拦，避免把报名记录写进软删/已结束的项目）
        Event event = eventMapper.selectById(eventId);
        if (event == null) {
            throw new RuntimeException("项目不存在或已删除");
        }
        if (p.getSportsMeetingId() != null && !p.getSportsMeetingId().equals(event.getSportsMeetingId())) {
            throw new RuntimeException("该项目不属于本运动会");
        }
        if (event.getStatus() != null && event.getStatus() == 1) {
            throw new RuntimeException("该项目已结束，无法报名");
        }

        // ② 资格链 participant→team→group_type：任一级缺失则组别/代表队维度的规则整体跳过，不拦截。
        //    没有组别和代表队的人只受项目自身规则(⑧)约束。
        Team team = (p.getTeamId() != null) ? teamMapper.selectById(p.getTeamId()) : null;
        Long groupTypeId = (team != null) ? team.getGroupTypeId() : null;
        GroupType gt = (groupTypeId != null) ? groupTypeMapper.selectById(groupTypeId) : null;

        // ③ 组别匹配(仅小程序端)：拿不到本人组别时无从比对，跳过
        if (strict && gt != null && event.getGroupTypeId() != null
                && !event.getGroupTypeId().equals(groupTypeId)) {
            throw new RuntimeException("该项目不属于你所在的组别，无法报名");
        }

        // ④ 轮次
        Long resolvedScheduleId = scheduleId;
        if (strict) {
            if (resolvedScheduleId == null) {
                resolvedScheduleId = eventScheduleService.getOpenScheduleIdByEventId(eventId);
                if (resolvedScheduleId == null) {
                    throw new RuntimeException("该项目当前未开放报名");
                }
            } else if (!eventScheduleService.isScheduleOpen(eventId, resolvedScheduleId)) {
                // 显式指定轮次时必须核实该轮真的开放，否则可绕过「未开放报名」的限制
                throw new RuntimeException("该项目当前未开放报名");
            }
        }

        // ⑤ 是否已报名（放在名额检查之前：重复点击时报「已报名」比报「名额已满」准确）
        if (participantId != null) {
            Registration existing = (resolvedScheduleId != null)
                    ? registrationMapper.selectByParticipantIdEventIdScheduleId(participantId, eventId, resolvedScheduleId)
                    : registrationMapper.selectByParticipantIdAndEventId(participantId, eventId);
            if (existing != null) {
                throw new RuntimeException("您已报名该项目");
            }
        }

        // ⑥ 规则A:每代表队在「代表队选中集」项目上限报人数（无代表队/无组别则跳过）
        if (gt != null) {
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

            // ⑦ 规则B:每人限报项目数(每人选中集内,只统计 status=0 的已报名)
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

        // ⑧ 规则C:项目自身报名上限(register_limit,0 表示不限)；复用①已加载的 event
        if (event.getRegisterLimit() != null && event.getRegisterLimit() > 0) {
            int cap = event.getRegisterLimit();
            if (registrationMapper.countByEventId(eventId) >= cap) {
                throw new RuntimeException("超出限报:该项目最多报 " + cap + " 人");
            }
        }

        return resolvedScheduleId;
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

    @Override
    public int promoteTopN(Long eventId, Long scheduleId, int topN) {
        // 按成绩排序取前 topN 名（排序规则同 ResultMapper：径赛/团队赛时间ASC，田赛距离DESC）
        List<ResultVO> results = resultService.getByEventAndSchedule(eventId, scheduleId);
        List<Long> participantIds = results.stream()
                .filter(r -> r.getScoreValue() != null)
                .limit(topN)
                .map(ResultVO::getParticipantId)
                .collect(Collectors.toList());
        int count = 0;
        for (Long pid : participantIds) {
            Registration reg = registrationMapper.selectByParticipantIdEventIdScheduleId(pid, eventId, scheduleId);
            if (reg != null && reg.getStatus() != null && reg.getStatus() != 1) {
                // 复用 update(id, 1) 触发 autoRegisterNextSchedule（下一赛次自动报名）
                update(reg.getId(), 1);
                count++;
            }
        }
        return count;
    }
}
