package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.ParticipantMapper;
import com.dlust.sportbackend.Service.ParticipantService;
import com.dlust.sportbackend.entity.Participant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParticipantServiceImpl implements ParticipantService {

    @Autowired
    private ParticipantMapper participantMapper;

    @Override
    public List<Participant> getBySportsMeetingId(Long sportsMeetingId) {
        return participantMapper.selectBySportsMeetingId(sportsMeetingId);
    }

    @Override
    public Participant getById(Long id) {
        return participantMapper.selectById(id);
    }

    @Override
    public void add(Participant participant) {
        participantMapper.insert(participant);
    }

    @Override
    public void update(Participant participant) {
        participantMapper.updateById(participant);
    }

    @Override
    public void delete(Long id) {
        participantMapper.deleteById(id);
    }
}
