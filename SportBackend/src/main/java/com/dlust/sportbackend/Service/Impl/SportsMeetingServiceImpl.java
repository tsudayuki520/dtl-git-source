package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Service.SportsMeetingService;
import com.dlust.sportbackend.Mapper.SportsMeetingMapper;
import com.dlust.sportbackend.entity.SportsMeeting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SportsMeetingServiceImpl implements SportsMeetingService {

    @Autowired
    private SportsMeetingMapper sportsMeetingMapper;

    @Override
    public List<SportsMeeting> getAll() {
        return sportsMeetingMapper.selectAll();
    }

    @Override
    public List<SportsMeeting> search(String keyword) {
        return sportsMeetingMapper.selectByKeyword(keyword);
    }

    @Override
    public SportsMeeting getById(Long id) {
        return sportsMeetingMapper.selectById(id);
    }

    @Override
    public void add(SportsMeeting sportsMeeting) {
        sportsMeetingMapper.insert(sportsMeeting);
    }

    @Override
    public void update(SportsMeeting sportsMeeting) {
        sportsMeetingMapper.updateById(sportsMeeting);
    }

    @Override
    public void delete(Long id) {
        sportsMeetingMapper.deleteById(id);
    }
}
