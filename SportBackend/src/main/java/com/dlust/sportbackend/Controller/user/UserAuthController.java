package com.dlust.sportbackend.Controller.user;

import com.dlust.sportbackend.Mapper.UserMapper;
import com.dlust.sportbackend.Service.UserService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.User;
import com.dlust.sportbackend.util.JwtUtil;
import com.dlust.sportbackend.util.PasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserAuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordService passwordService;

    /**
     * 账号密码登录：校验 userCode + BCrypt 密码后签发 JWT（payload=userId）。
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String userCode = body.get("userCode");
        String password = body.get("password");
        if (userCode == null || password == null) {
            return Result.error(400, "账号密码不能为空");
        }
        User u = userService.loginByUserCode(userCode, password);
        String token = JwtUtil.generateToken(u.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", u.getId());
        data.put("userCode", u.getUserCode());
        data.put("name", u.getName());
        return Result.success(data);
    }

    /**
     * 修改密码：需登录，校验原密码后写入新密码的 BCrypt 哈希。
     */
    @PostMapping("/change-password")
    public Result<String> changePassword(@RequestAttribute("userId") Long userId,
                                         @RequestBody Map<String, String> body) {
        String oldPwd = body.get("oldPassword");
        String newPwd = body.get("newPassword");
        if (oldPwd == null || newPwd == null || newPwd.isEmpty()) {
            return Result.error(400, "参数缺失");
        }
        User u = userMapper.selectById(userId);
        if (u == null || !passwordService.matches(oldPwd, u.getPassword())) {
            return Result.error(400, "原密码错误");
        }
        userMapper.updatePassword(userId, passwordService.encode(newPwd));
        return Result.success("修改成功");
    }
}
