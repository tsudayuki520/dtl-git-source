package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.Participant;
import com.dlust.sportbackend.entity.RegistrationVO;
import java.util.List;

public interface RegistrationService {

    List<RegistrationVO> getBySportsMeetingId(Long sportsMeetingId);

    List<RegistrationVO> getByEventId(Long eventId);

    List<RegistrationVO> getByParticipantId(Long participantId);

    /**
     * 报名（已知 participantId；admin 端使用，无法做代表队资格校验因 selectById 已移除）。
     */
    void add(Long participantId, Long eventId, Long scheduleId);

    /**
     * 报名（Controller 已加载好 Participant，复用其 teamId 做限报校验）。
     */
    void add(Participant participant, Long eventId, Long scheduleId);

    void update(Long id, Integer status);

    void delete(Long id);

    int promoteTopN(Long eventId, Long scheduleId, int topN);
}
