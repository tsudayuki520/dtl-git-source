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

    EventSchedule selectByEventIdAndScheduleId(@Param("eventId") Long eventId, @Param("scheduleId") Long scheduleId);

    void insert(@Param("eventId") Long eventId, @Param("scheduleId") Long scheduleId);

    void batchInsertWithAllow(@Param("list") List<EventSchedule> list);

    /** 按给定 scheduleIds 集合，依 schedule.sort 升序返回 */
    List<Long> selectScheduleIdsByEventIdOrdered(@Param("scheduleIds") List<Long> scheduleIds);

    /** 软删除某 event 下的某个 schedule 关联 */
    void deleteByScheduleIdForEvent(@Param("eventId") Long eventId, @Param("scheduleId") Long scheduleId);

    void updateAllowRegister(@Param("eventId") Long eventId,
                             @Param("scheduleId") Long scheduleId,
                             @Param("allowRegister") Integer allowRegister);

    /** 取该 event 下 allow_register=1 且 sort 最小的 scheduleId；无则返回 null */
    Long selectOpenScheduleIdByEventId(@Param("eventId") Long eventId);

    void deleteByEventId(@Param("eventId") Long eventId);

    void deleteById(@Param("id") Long id);

    void deleteByScheduleId(@Param("scheduleId") Long scheduleId);
}
