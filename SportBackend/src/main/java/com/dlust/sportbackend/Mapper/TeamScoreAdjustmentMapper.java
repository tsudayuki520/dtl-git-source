package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.TeamScoreAdjustment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TeamScoreAdjustmentMapper {

    List<TeamScoreAdjustment> selectByTeamId(@Param("teamId") Long teamId);

    void insert(TeamScoreAdjustment adjustment);

    void deleteById(@Param("id") Long id);
}
