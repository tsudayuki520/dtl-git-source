package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.Result;
import com.dlust.sportbackend.entity.ResultVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResultMapper {

    List<ResultVO> selectVOBySportsMeetingId(@Param("sportsMeetingId") Long sportsMeetingId);

    List<ResultVO> selectVOByEventId(@Param("eventId") Long eventId);

    List<ResultVO> selectVOByEventAndSchedule(@Param("eventId") Long eventId, @Param("scheduleId") Long scheduleId);

    void insert(Result result);

    void updateById(Result result);

    void deleteById(@Param("id") Long id);
}
