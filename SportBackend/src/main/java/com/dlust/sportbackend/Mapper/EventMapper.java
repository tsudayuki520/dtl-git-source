package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.Event;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface EventMapper {

    List<Event> selectBySportsMeetingId(@Param("sportsMeetingId") Long sportsMeetingId);

    Event selectById(@Param("id") Long id);

    void insert(Event event);

    void updateById(Event event);

    void softDeleteById(@Param("id") Long id);
}
