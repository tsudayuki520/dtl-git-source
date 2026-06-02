package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.Participant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ParticipantMapper {

    Participant selectById(@Param("id") Long id);

    Participant selectByUserCodeAndSportsMeetingId(@Param("userCode") String userCode,
                                                    @Param("sportsMeetingId") Long sportsMeetingId);

    void insert(Participant participant);
}
