package com.dlust.sportbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String openid;
    private String unionId;
    private String nickname;
    private String avatarUrl;
    private String phone;
    private String sessionKey;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
