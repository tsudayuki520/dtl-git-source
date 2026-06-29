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

    /** 查询某 (eventId, scheduleId) 是否存在软删除行（用于复活前判断，不过滤 is_deleted=0） */
    EventSchedule selectDeletedByEventIdAndScheduleId(@Param("eventId") Long eventId, @Param("scheduleId") Long scheduleId);

    /** 复活软删除行并更新 allow_register */
    void reviveByEventIdAndScheduleId(@Param("eventId") Long eventId,
                                      @Param("scheduleId") Long scheduleId,
                                      @Param("allowRegister") Integer allowRegister);

    /** 返回受影响行数：0 表示该关联不存在或已被软删 */
    int updateAllowRegister(@Param("eventId") Long eventId,
                            @Param("scheduleId") Long scheduleId,
                            @Param("allowRegister") Integer allowRegister);

    /** 取该 event 下 allow_register=1 且 sort 最小的 scheduleId；无则返回 null */
    Long selectOpenScheduleIdByEventId(@Param("eventId") Long eventId);

    void deleteByEventId(@Param("eventId") Long eventId);

    void deleteById(@Param("id") Long id);

    void deleteByScheduleId(@Param("scheduleId") Long scheduleId);
}
