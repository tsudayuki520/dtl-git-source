package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.Participant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ParticipantMapper {

    List<Participant> selectBySportsMeetingId(@Param("sportsMeetingId") Long sportsMeetingId);

    List<Participant> selectByTeamId(@Param("teamId") Long teamId);

    Participant selectByUserIdAndSportsMeetingId(@Param("userId") Long userId,
                                                  @Param("sportsMeetingId") Long sportsMeetingId);

    /** 按 participant.id 查询并 JOIN user 表取 name/userCode 等（仅 RecordServiceImpl 入册时使用）。 */
    Participant selectByIdWithUser(@Param("id") Long id);

    int insert(Participant participant);

    int updateTeamId(@Param("id") Long id, @Param("teamId") Long teamId);

    int clearTeamId(@Param("id") Long id);

    int deleteById(@Param("id") Long id);
}
