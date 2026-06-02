package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.Registration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface RegistrationMapper {

    Registration selectByParticipantIdAndEventId(@Param("participantId") Long participantId,
                                                 @Param("eventId") Long eventId);

    void insert(Registration registration);

    int countByEventId(@Param("eventId") Long eventId);

    List<Map<String, Object>> countBySportsMeetingId(@Param("sportsMeetingId") Long sportsMeetingId);
}
