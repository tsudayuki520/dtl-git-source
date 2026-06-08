package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.Participant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ParticipantMapper {

    Participant selectById(@Param("id") Long id);

    Participant selectByUserCodeAndSportsMeetingId(@Param("userCode") String userCode,
                                                    @Param("sportsMeetingId") Long sportsMeetingId);

    List<Participant> selectBySportsMeetingId(@Param("sportsMeetingId") Long sportsMeetingId);

    List<Participant> selectByTeamId(@Param("teamId") Long teamId);

    void insert(Participant participant);

    void updateById(Participant participant);

    void deleteById(@Param("id") Long id);
}
