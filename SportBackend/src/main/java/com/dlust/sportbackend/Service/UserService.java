package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.User;

public interface UserService {

    /**
     * 账号密码登录：按 userCode 查用户并校验 BCrypt 密码。
     * 账号不存在或密码错误时抛出 RuntimeException（由全局异常处理器转 400）。
     */
    User loginByUserCode(String userCode, String password);

    User getById(Long id);

    void updatePhone(Long userId, String phone);

    /**
     * 管理端重置密码：直接写入已哈希的密码。
     */
    void resetPassword(Long userId, String passwordHash);
}
