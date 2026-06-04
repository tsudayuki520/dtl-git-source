package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.RegistrationMapper;
import com.dlust.sportbackend.Service.RegistrationService;
import com.dlust.sportbackend.entity.RegistrationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    @Autowired
    private RegistrationMapper registrationMapper;

    @Override
    public List<RegistrationVO> getBySportsMeetingId(Long sportsMeetingId) {
        return registrationMapper.selectVOBySportsMeetingId(sportsMeetingId);
    }

    @Override
    public void update(Long id, Integer status) {
        registrationMapper.updateStatus(id, status);
    }

    @Override
    public void delete(Long id) {
        registrationMapper.deleteById(id);
    }
}
