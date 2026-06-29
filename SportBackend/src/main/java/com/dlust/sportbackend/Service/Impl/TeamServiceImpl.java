package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.TeamMapper;
import com.dlust.sportbackend.Service.TeamService;
import com.dlust.sportbackend.entity.Team;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamServiceImpl implements TeamService {

    @Autowired
    private TeamMapper teamMapper;

    @Override
    public List<Team> getBySportsMeetingId(Long sportsMeetingId) {
        return teamMapper.selectBySportsMeetingId(sportsMeetingId);
    }

    @Override
    public List<Team> getByGroupTypeId(Long groupTypeId) {
        return teamMapper.selectByGroupTypeId(groupTypeId);
    }

    @Override
    public Team getById(Long id) {
        return teamMapper.selectById(id);
    }

    @Override
    public void add(Team team) {
        teamMapper.insert(team);
    }

    @Override
    public void update(Team team) {
        teamMapper.updateById(team);
    }

    @Override
    public void delete(Long id) {
        teamMapper.deleteById(id);
    }

    @Override
    public int refreshTotalScoreBySportsMeetingId(Long sportsMeetingId) {
        if (sportsMeetingId == null) {
            throw new RuntimeException("运动会 ID 不能为空");
        }
        return teamMapper.recalculateTotalScoreBySportsMeetingId(sportsMeetingId);
    }
}
