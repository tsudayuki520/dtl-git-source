package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.TeamScoreAdjustmentMapper;
import com.dlust.sportbackend.Service.TeamScoreAdjustmentService;
import com.dlust.sportbackend.entity.TeamScoreAdjustment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TeamScoreAdjustmentServiceImpl implements TeamScoreAdjustmentService {

    @Autowired
    private TeamScoreAdjustmentMapper teamScoreAdjustmentMapper;

    @Override
    public List<TeamScoreAdjustment> getByTeamId(Long teamId) {
        return teamScoreAdjustmentMapper.selectByTeamId(teamId);
    }

    @Override
    public void add(Long teamId, BigDecimal deltaAmount, String note) {
        validateAdd(teamId, deltaAmount, note);
        TeamScoreAdjustment adj = new TeamScoreAdjustment();
        adj.setTeamId(teamId);
        adj.setDeltaAmount(deltaAmount);
        adj.setNote(note);
        teamScoreAdjustmentMapper.insert(adj);
    }

    @Override
    public void delete(Long id) {
        teamScoreAdjustmentMapper.deleteById(id);
    }

    /**
     * 抽成包级静态方法以便单测（不依赖 Spring/Mapper）。
     * 校验：teamId/deltaAmount 非 null；note 非空。
     */
    static void validateAdd(Long teamId, BigDecimal deltaAmount, String note) {
        if (teamId == null) {
            throw new RuntimeException("代表队 ID 不能为空");
        }
        if (deltaAmount == null) {
            throw new RuntimeException("调整数额不能为空");
        }
        if (note == null || note.trim().isEmpty()) {
            throw new RuntimeException("调整原因不能为空");
        }
    }
}
