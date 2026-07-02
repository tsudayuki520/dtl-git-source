package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.Participant;
import java.util.List;

public interface ParticipantService {

    List<Participant> getBySportsMeetingId(Long sportsMeetingId);

    List<Participant> getByTeamId(Long teamId);

    Participant getById(Long id);

    /**
     * 新增参赛人员：upsert user（按 userCode 查找；不存在则建，默认密码 BCrypt("dlust123456")；
     * 存在则用请求中的非空字段更新 user 资料）+ upsert participant 关联（按 userId+sportsMeetingId
     * 查找；不存在则插入，存在则跳过，幂等）。
     * 入参 Participant 携带：sportsMeetingId、userCode、name、gender、phone、college、major。
     */
    void add(Participant participant);

    /**
     * 编辑参赛人员对应的 user 资料（name/gender/phone/college/major）。
     * participant 行通过 user_id 关联，不会变更。
     * team_id 不在此处管理（由代表队分配接口负责）。
     * 入参 Participant 携带：id（participant.id）、name、gender、phone、college、major。
     */
    void update(Participant participant);

    void delete(Long id);

    void clearTeamId(Long id);
}
