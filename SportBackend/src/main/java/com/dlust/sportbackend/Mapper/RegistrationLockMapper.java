package com.dlust.sportbackend.Mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RegistrationLockMapper {
    @Select("SELECT id FROM user WHERE id = #{id} FOR UPDATE")
    Long lockUser(@Param("id") Long id);

    @Select("SELECT id FROM event WHERE id = #{id} FOR UPDATE")
    Long lockEvent(@Param("id") Long id);
}
