package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.UserMapper;
import com.dlust.sportbackend.Service.UserService;
import com.dlust.sportbackend.entity.User;
import com.dlust.sportbackend.util.PasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordService passwordService;

    @Override
    public User loginByUserCode(String userCode, String password) {
        User u = userMapper.selectByUserCode(userCode);
        if (u == null) {
            throw new RuntimeException("账号不存在");
        }
        if (!passwordService.matches(password, u.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        return u;
    }

    @Override
    public void resetPassword(Long userId, String passwordHash) {
        userMapper.updatePassword(userId, passwordHash);
    }
}
