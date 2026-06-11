package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.EventSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EventScheduleMapper {

    List<EventSchedule> selectByEventId(@Param("eventId") Long eventId);

    List<EventSchedule> selectByScheduleId(@Param("scheduleId") Long scheduleId);

    List<Long> selectScheduleIdsByEventId(@Param("eventId") Long eventId);

    List<Long> selectEventIdsByScheduleId(@Param("scheduleId") Long scheduleId);

    List<EventSchedule> selectBySportsMeetingId(@Param("sportsMeetingId") Long sportsMeetingId);

    void insert(@Param("eventId") Long eventId, @Param("scheduleId") Long scheduleId);

    void batchInsert(@Param("eventId") Long eventId, @Param("scheduleIds") List<Long> scheduleIds);

    void deleteByEventId(@Param("eventId") Long eventId);

    void deleteById(@Param("id") Long id);

    void deleteByScheduleId(@Param("scheduleId") Long scheduleId);
}
