package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.Registration;
import com.dlust.sportbackend.entity.RegistrationVO;
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

    List<RegistrationVO> selectVOBySportsMeetingId(@Param("sportsMeetingId") Long sportsMeetingId);

    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    void deleteById(@Param("id") Long id);
}
