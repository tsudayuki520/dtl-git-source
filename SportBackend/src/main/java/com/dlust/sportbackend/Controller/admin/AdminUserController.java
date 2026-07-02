package com.dlust.sportbackend.Controller.admin;

import com.dlust.sportbackend.Service.UserService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.util.PasswordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordService passwordService;

    @PostMapping("/reset-password")
    public Result<String> resetPassword(@RequestBody Map<String, Object> body) {
        Object idObj = body.get("userId");
        if (idObj == null) {
            return Result.error(400, "userId 缺失");
        }
        Long userId = ((Number) idObj).longValue();
        log.info("重置用户密码: userId={}", userId);
        userService.resetPassword(userId, passwordService.encode("dlust123456"));
        return Result.success("已重置为默认密码 dlust123456");
    }
}
