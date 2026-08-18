package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.Team;
import com.dlust.sportbackend.entity.TeamVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TeamMapper {

    List<Team> selectBySportsMeetingId(@Param("sportsMeetingId") Long sportsMeetingId);

    /** 查某 user 所属的所有代表队（跨运动会，JOIN sports_meeting 带名称，含成员数）。 */
    List<TeamVO> selectByUserId(@Param("userId") Long userId);

    List<Team> selectByGroupTypeId(@Param("groupTypeId") Long groupTypeId);

    Team selectById(@Param("id") Long id);

    void insert(Team team);

    void updateById(Team team);

    int recalculateTotalScoreBySportsMeetingId(@Param("sportsMeetingId") Long sportsMeetingId);

    void deleteById(@Param("id") Long id);
}
