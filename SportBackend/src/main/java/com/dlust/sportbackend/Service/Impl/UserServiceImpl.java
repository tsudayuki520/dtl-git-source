package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.UserMapper;
import com.dlust.sportbackend.Service.UserService;
import com.dlust.sportbackend.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User loginByOpenid(String openid, String sessionKey) {
        User user = userMapper.selectByOpenid(openid);
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setSessionKey(sessionKey);
            userMapper.insert(user);
        } else {
            // 每次登录更新 session_key
            user.setSessionKey(sessionKey);
            userMapper.updateById(user);
        }
        return user;
    }

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public void updatePhone(Long userId, String phone) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPhone(phone);
            userMapper.updateById(user);
        }
    }
}
