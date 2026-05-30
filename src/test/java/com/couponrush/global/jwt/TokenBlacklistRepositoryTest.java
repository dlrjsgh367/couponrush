package com.couponrush.global.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@DataRedisTest
class TokenBlacklistRepositoryTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private TokenBlacklistRepository repository;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        repository = new TokenBlacklistRepository(redisTemplate);
    }

    @Test
    void 등록되지_않은_토큰은_블랙리스트가_아니다() {
        assertThat(repository.isBlacklisted("free-token")).isFalse();
    }

    @Test
    void 등록한_토큰은_블랙리스트다() {
        repository.blacklist("logged-out-token", 60_000L);

        assertThat(repository.isBlacklisted("logged-out-token")).isTrue();
    }

    @Test
    void 등록시_잔여시간만큼_TTL이_설정된다() {
        repository.blacklist("ttl-token", 60_000L);

        Long ttl = redisTemplate.getExpire("jwt:blacklist:ttl-token", TimeUnit.MILLISECONDS);

        assertThat(ttl).isPositive().isLessThanOrEqualTo(60_000L);
    }
}
