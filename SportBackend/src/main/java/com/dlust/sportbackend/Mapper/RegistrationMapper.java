package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.Registration;
import com.dlust.sportbackend.entity.RegistrationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface RegistrationMapper {

    Registration selectById(@Param("id") Long id);

    Registration selectByParticipantIdEventIdScheduleId(@Param("participantId") Long participantId,
                                                        @Param("eventId") Long eventId,
                                                        @Param("scheduleId") Long scheduleId);

    Registration selectByParticipantIdAndEventId(@Param("participantId") Long participantId,
                                                 @Param("eventId") Long eventId);

    void insert(Registration registration);

    int countByEventId(@Param("eventId") Long eventId);

    List<Map<String, Object>> countBySportsMeetingId(@Param("sportsMeetingId") Long sportsMeetingId);

    List<RegistrationVO> selectVOBySportsMeetingId(@Param("sportsMeetingId") Long sportsMeetingId);

    List<RegistrationVO> selectVOByEventId(@Param("eventId") Long eventId);

    List<RegistrationVO> selectVOByParticipantId(@Param("participantId") Long participantId);

    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int countDistinctEventByParticipantInEvents(@Param("participantId") Long participantId,
                                                 @Param("eventIds") List<Long> eventIds,
                                                 @Param("statuses") List<Integer> statuses);

    int countDistinctParticipantByTeamAndEvent(@Param("teamId") Long teamId,
                                                @Param("eventId") Long eventId,
                                                @Param("statuses") List<Integer> statuses);

    void deleteById(@Param("id") Long id);
}
