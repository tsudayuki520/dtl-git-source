package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.Team;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TeamMapper {

    List<Team> selectBySportsMeetingId(@Param("sportsMeetingId") Long sportsMeetingId);

    List<Team> selectByGroupTypeId(@Param("groupTypeId") Long groupTypeId);

    Team selectById(@Param("id") Long id);

    void insert(Team team);

    void updateById(Team team);

    int recalculateTotalScoreBySportsMeetingId(@Param("sportsMeetingId") Long sportsMeetingId);

    void deleteById(@Param("id") Long id);
}
