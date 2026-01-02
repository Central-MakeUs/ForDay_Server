package com.example.ForDay.domain.auth.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash(value = "refreshToken", timeToLive = 60 * 60 * 24 * 7) // 7일
public class RefreshToken {

    @Id
    private Long userId;   // socialId (또는 userId)
    private String token;

    public RefreshToken(Long userId, String token) {
        this.userId = userId;
        this.token = token;
    }

    public Long getUserId() { return userId; }

    public String getToken() { return token; }

    public void update(String token) {
        this.token = token;
    }
}
