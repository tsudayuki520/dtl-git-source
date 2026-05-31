package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.User;

public interface UserService {

    /**
     * 根据 openid 查找用户，不存在则自动创建
     * @return 用户实体
     */
    User loginByOpenid(String openid, String sessionKey);

    User getById(Long id);

    void updatePhone(Long userId, String phone);
}
