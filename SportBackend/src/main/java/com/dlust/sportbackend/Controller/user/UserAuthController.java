package com.dlust.sportbackend.Controller.user;

import com.dlust.sportbackend.Service.UserService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.User;
import com.dlust.sportbackend.util.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserAuthController {

    private static final Logger log = LoggerFactory.getLogger(UserAuthController.class);
    @Autowired
    private UserService userService;

    @Value("${wechat.appid:}")
    private String appId;

    @Value("${wechat.secret:}")
    private String appSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 小程序登录：用 code 换取 openid，自动注册/登录用户，返回 token
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        log.info("登录接口被调用");
        String code = body.get("code");
        if (code == null || code.isEmpty()) {
            return Result.error(400, "code 不能为空");
        }

        String wxUrl = "https://api.weixin.qq.com/sns/jscode2session?appid=" + appId
                + "&secret=" + appSecret
                + "&js_code=" + code
                + "&grant_type=authorization_code";

        try {
            String wxResult = httpGet(wxUrl);
            JsonNode json = objectMapper.readTree(wxResult);

            if (json.has("errcode") && json.get("errcode").asInt() != 0) {
                return Result.error(500, "微信登录失败: " + json.get("errmsg").asText());
            }

            String openid = json.get("openid").asText();
            String sessionKey = json.get("session_key").asText();

            User user = userService.loginByOpenid(openid, sessionKey);
            String token = JwtUtil.generateToken(user.getId());

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userId", user.getId());
            data.put("nickname", user.getNickname());
            data.put("avatarUrl", user.getAvatarUrl());
            data.put("phone", user.getPhone());
            return Result.success(data);

        } catch (Exception e) {
            return Result.error(500, "登录异常: " + e.getMessage());
        }
    }

    /**
     * 绑定手机号
     */
    @PostMapping("/phone")
    public Result<String> bindPhone(@RequestBody Map<String, String> body,
                                     @RequestAttribute("userId") Long userId) {
        log.info("手机号接口被调用");
        String code = body.get("code");
        if (code == null || code.isEmpty()) {
            return Result.error(400, "code 不能为空");
        }

        String tokenUrl = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                + appId + "&secret=" + appSecret;
        try {
            String tokenResult = httpGet(tokenUrl);
            JsonNode tokenJson = objectMapper.readTree(tokenResult);
            String accessToken = tokenJson.get("access_token").asText();

            String phoneUrl = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + accessToken;
            String phoneBody = "{\"code\":\"" + code + "\"}";
            String phoneResult = httpPost(phoneUrl, phoneBody);
            JsonNode phoneJson = objectMapper.readTree(phoneResult);

            if (phoneJson.get("errcode").asInt() != 0) {
                return Result.error(500, "获取手机号失败");
            }

            String phoneNumber = phoneJson.get("phone_info").get("phoneNumber").asText();
            userService.updatePhone(userId, phoneNumber);
            return Result.success("绑定成功");

        } catch (Exception e) {
            return Result.error(500, "绑定手机号异常: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<User> getUserInfo(@RequestAttribute("userId") Long userId) {
        log.info("获取用户信息接口被调用");
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(user);
    }

    /**
     * 上传头像到 OBS 并更新 user 表
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestAttribute("userId") Long userId,
                                        @RequestParam("file") MultipartFile file) {
        log.info("上传头像: userId={}", userId);
        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }
        try {
            String originalName = file.getOriginalFilename();
            String ext = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf("."))
                    : ".png";
            String objectKey = "user_images/" + userId + "_" + System.currentTimeMillis() + ext;
            String url = com.dlust.sportbackend.util.OBSUtil.uploadFile(objectKey, file.getInputStream());
            if (url == null) {
                return Result.error(500, "上传失败");
            }
            userService.updateAvatar(userId, url);
            return Result.success(url);
        } catch (Exception e) {
            return Result.error(500, "上传异常: " + e.getMessage());
        }
    }

    /**
     * 更新昵称
     */
    @PutMapping("/nickname")
    public Result<String> updateNickname(@RequestAttribute("userId") Long userId,
                                          @RequestBody Map<String, String> body) {
        log.info("更新昵称: userId={}", userId);
        String nickname = body.get("nickname");
        if (nickname == null || nickname.trim().isEmpty()) {
            return Result.error(400, "昵称不能为空");
        }
        userService.updateNickname(userId, nickname.trim());
        return Result.success("更新成功");
    }

    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    private String httpPost(String urlStr, String body) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.getOutputStream().write(body.getBytes());
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
}
