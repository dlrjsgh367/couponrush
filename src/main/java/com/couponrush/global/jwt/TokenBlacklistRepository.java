package com.couponrush.global.jwt;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TokenBlacklistRepository {

    private static final String KEY_PREFIX = "jwt:blacklist:";
    private static final String LOGOUT_MARK = "logout";

    private final StringRedisTemplate redisTemplate;

    public void blacklist(String token, long ttlMillis) {
        redisTemplate.opsForValue().set(KEY_PREFIX + token, LOGOUT_MARK, Duration.ofMillis(ttlMillis));
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + token));
    }
}
