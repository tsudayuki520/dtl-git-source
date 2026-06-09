package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.GroupType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupTypeMapper {

    List<GroupType> selectBySportsMeetingId(@Param("sportsMeetingId") Long sportsMeetingId);

    GroupType selectById(@Param("id") Long id);

    void insert(GroupType groupType);

    void updateById(GroupType groupType);

    void deleteById(@Param("id") Long id);
}
