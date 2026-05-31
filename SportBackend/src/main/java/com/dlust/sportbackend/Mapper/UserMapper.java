package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User selectByOpenid(@Param("openid") String openid);

    User selectById(@Param("id") Long id);

    void insert(User user);

    void updateById(User user);
}
