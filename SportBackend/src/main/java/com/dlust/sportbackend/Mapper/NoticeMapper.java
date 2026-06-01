package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface NoticeMapper {

    List<Notice> selectGlobal();

    List<Notice> selectBySportsMeetingId(@Param("sportsMeetingId") Long sportsMeetingId);

    List<Notice> selectAll();

    Notice selectById(@Param("id") Long id);

    void insert(Notice notice);

    void updateById(Notice notice);

    void deleteById(@Param("id") Long id);
}
