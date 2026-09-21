package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.Participant;
import com.dlust.sportbackend.entity.RegistrationVO;
import java.util.List;
import java.util.Map;

public interface RegistrationService {

    List<RegistrationVO> getBySportsMeetingId(Long sportsMeetingId);

    List<RegistrationVO> getByEventId(Long eventId);

    List<RegistrationVO> getByParticipantId(Long participantId);

    /** 某运动会下 eventId -> 已报名人数(status=0) */
    Map<Long, Integer> countBySportsMeetingId(Long sportsMeetingId);

    /**
     * 报名（管理端入口，已知 participantId）。
     * 管理员可指定任意人参加任意项目，故跳过「项目组别匹配」与「轮次必须开放」两项校验，
     * 但项目有效性（未软删/未结束/同运动会）、代表队资格与限报规则照常校验。
     */
    void add(Long participantId, Long eventId, Long scheduleId);

    /**
     * 报名（小程序端入口，Controller 已按 (userId, sportsMeetingId) 加载或新建 Participant）。
     * 比管理端多两项校验：项目必须面向报名人所在组别；轮次必须确实开放。
     * Participant 若尚未落库，会在校验全部通过后才 insert。
     */
    void add(Participant participant, Long eventId, Long scheduleId);

    void update(Long id, Integer status);

    void delete(Long id);

    int promoteTopN(Long eventId, Long scheduleId, int topN);
}
