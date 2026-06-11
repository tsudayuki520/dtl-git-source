package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.RegistrationMapper;
import com.dlust.sportbackend.Service.EventScheduleService;
import com.dlust.sportbackend.Service.RegistrationService;
import com.dlust.sportbackend.entity.Registration;
import com.dlust.sportbackend.entity.RegistrationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class RegistrationServiceImpl implements RegistrationService {

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private EventScheduleService eventScheduleService;

    @Override
    public List<RegistrationVO> getBySportsMeetingId(Long sportsMeetingId) {
        return registrationMapper.selectVOBySportsMeetingId(sportsMeetingId);
    }

    @Override
    public List<RegistrationVO> getByEventId(Long eventId) {
        return registrationMapper.selectVOByEventId(eventId);
    }

    @Override
    public void update(Long id, Integer status) {
        registrationMapper.updateStatus(id, status);

        // 晋级时自动报名下一赛次
        if (status == 1) {
            autoRegisterNextSchedule(id);
        }
    }

    private void autoRegisterNextSchedule(Long registrationId) {
        Registration reg = registrationMapper.selectById(registrationId);
        if (reg == null || reg.getEventId() == null || reg.getScheduleId() == null) return;

        // 获取该项目所有赛次（按创建时间排序）
        List<Long> scheduleIds = eventScheduleService.getScheduleIdsByEventId(reg.getEventId());
        if (scheduleIds == null || scheduleIds.isEmpty()) return;

        // 找到当前赛次的位置
        int currentIndex = scheduleIds.indexOf(reg.getScheduleId());
        if (currentIndex < 0 || currentIndex >= scheduleIds.size() - 1) return;

        // 下一赛次
        Long nextScheduleId = scheduleIds.get(currentIndex + 1);

        // 检查是否已报名
        Registration existing = registrationMapper.selectByParticipantIdEventIdScheduleId(
                reg.getParticipantId(), reg.getEventId(), nextScheduleId);
        if (existing != null) return;

        // 自动报名下一赛次
        Registration newReg = new Registration();
        newReg.setParticipantId(reg.getParticipantId());
        newReg.setEventId(reg.getEventId());
        newReg.setScheduleId(nextScheduleId);
        newReg.setStatus(0);
        registrationMapper.insert(newReg);
        log.info("晋级自动报名: participantId={}, eventId={}, nextScheduleId={}",
                reg.getParticipantId(), reg.getEventId(), nextScheduleId);
    }

    @Override
    public void delete(Long id) {
        registrationMapper.deleteById(id);
    }
}
