package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.SportsMeeting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SportsMeetingMapper {

    List<SportsMeeting> selectAll();

    List<SportsMeeting> selectByKeyword(@Param("keyword") String keyword);

    SportsMeeting selectById(@Param("id") Long id);

    void insert(SportsMeeting sportsMeeting);

    void updateById(SportsMeeting sportsMeeting);

    void deleteById(@Param("id") Long id);
}
