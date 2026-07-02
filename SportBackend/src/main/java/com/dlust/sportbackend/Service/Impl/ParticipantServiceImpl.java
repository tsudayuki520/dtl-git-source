package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.ParticipantMapper;
import com.dlust.sportbackend.Mapper.UserMapper;
import com.dlust.sportbackend.Service.ParticipantService;
import com.dlust.sportbackend.entity.Participant;
import com.dlust.sportbackend.entity.User;
import com.dlust.sportbackend.util.PasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParticipantServiceImpl implements ParticipantService {

    @Autowired
    private ParticipantMapper participantMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordService passwordService;

    @Override
    public List<Participant> getBySportsMeetingId(Long sportsMeetingId) {
        return participantMapper.selectBySportsMeetingId(sportsMeetingId);
    }

    @Override
    public List<Participant> getByTeamId(Long teamId) {
        return participantMapper.selectByTeamId(teamId);
    }

    @Override
    public Participant getById(Long id) {
        return participantMapper.selectByIdWithUser(id);
    }

    @Override
    public void add(Participant participant) {
        Long sportsMeetingId = participant.getSportsMeetingId();
        String userCode = participant.getUserCode();
        if (sportsMeetingId == null || userCode == null || userCode.isBlank()) {
            throw new RuntimeException("sportsMeetingId 与 userCode 必填");
        }

        // 1. upsert user
        User user = userMapper.selectByUserCode(userCode);
        if (user == null) {
            user = new User();
            user.setUserCode(userCode);
            user.setPassword(passwordService.encode("dlust123456"));
            applyProfile(user, participant);
            userMapper.insert(user);
        } else {
            boolean dirty = applyProfileIfPresent(user, participant);
            if (dirty) {
                userMapper.updateProfile(user);
            }
        }

        // 2. upsert participant 链接（幂等）
        Participant existing = participantMapper.selectByUserIdAndSportsMeetingId(
                user.getId(), sportsMeetingId);
        if (existing == null) {
            Participant p = new Participant();
            p.setSportsMeetingId(sportsMeetingId);
            p.setUserId(user.getId());
            participantMapper.insert(p);
        }
    }

    @Override
    public void update(Participant participant) {
        if (participant.getId() == null) {
            throw new RuntimeException("participant id 缺失");
        }
        // 加载 participant 以拿到 userId（仅编辑 user 资料，participant 行不变）
        Participant loaded = participantMapper.selectByIdWithUser(participant.getId());
        if (loaded == null) {
            throw new RuntimeException("参赛人员不存在: id=" + participant.getId());
        }
        if (loaded.getUserId() == null) {
            throw new RuntimeException("参赛人员未关联用户: id=" + participant.getId());
        }
        User user = new User();
        user.setId(loaded.getUserId());
        applyProfileIfPresent(user, participant);
        userMapper.updateProfile(user);
    }

    @Override
    public void delete(Long id) {
        participantMapper.deleteById(id);
    }

    @Override
    public void clearTeamId(Long id) {
        participantMapper.clearTeamId(id);
    }

    /**
     * 用 participant 上的扩展字段（来自请求体）覆盖 user 的资料字段（无空判断，用于新建场景）。
     */
    private void applyProfile(User user, Participant p) {
        user.setName(p.getName());
        user.setGender(p.getGender());
        user.setPhone(p.getPhone());
        user.setCollege(p.getCollege());
        user.setMajor(p.getMajor());
    }

    /**
     * 仅当 participant 对应字段非空时覆盖 user 资料，返回是否有字段被更新（用于 updateProfile 调用判断）。
     * 用于编辑场景：user 已存在，按请求中提供的字段做局部更新。
     */
    private boolean applyProfileIfPresent(User user, Participant p) {
        boolean dirty = false;
        if (p.getName() != null) { user.setName(p.getName()); dirty = true; }
        if (p.getGender() != null) { user.setGender(p.getGender()); dirty = true; }
        if (p.getPhone() != null) { user.setPhone(p.getPhone()); dirty = true; }
        if (p.getCollege() != null) { user.setCollege(p.getCollege()); dirty = true; }
        if (p.getMajor() != null) { user.setMajor(p.getMajor()); dirty = true; }
        return dirty;
    }
}
