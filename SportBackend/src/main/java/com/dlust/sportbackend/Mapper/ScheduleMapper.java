package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ScheduleMapper {

    List<Schedule> selectBySportsMeetingId(@Param("sportsMeetingId") Long sportsMeetingId);

    Schedule selectById(@Param("id") Long id);

    void insert(Schedule schedule);

    void updateById(Schedule schedule);

    void softDeleteById(@Param("id") Long id);
}
