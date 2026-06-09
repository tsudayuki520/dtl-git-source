package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.ResultMapper;
import com.dlust.sportbackend.Service.ResultService;
import com.dlust.sportbackend.entity.Result;
import com.dlust.sportbackend.entity.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResultServiceImpl implements ResultService {

    @Autowired
    private ResultMapper resultMapper;

    @Override
    public List<ResultVO> getBySportsMeetingId(Long sportsMeetingId) {
        return resultMapper.selectVOBySportsMeetingId(sportsMeetingId);
    }

    @Override
    public List<ResultVO> getByEventId(Long eventId) {
        return resultMapper.selectVOByEventId(eventId);
    }

    @Override
    public void add(Result result) {
        resultMapper.insert(result);
    }

    @Override
    public void update(Result result) {
        resultMapper.updateById(result);
    }

    @Override
    public void delete(Long id) {
        resultMapper.deleteById(id);
    }
}
