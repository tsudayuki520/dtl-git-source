package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    User selectByUserCode(@Param("userCode") String userCode);
    User selectById(@Param("id") Long id);
    int insert(User user);
    int updatePassword(@Param("id") Long id, @Param("password") String password);
    int updateProfile(User user);  // 更新 name/gender/phone/college/major（不含 userCode/password）
    // 迁移用
    int insertForMigration(User user);
}
