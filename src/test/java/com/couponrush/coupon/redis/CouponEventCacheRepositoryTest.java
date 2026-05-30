package com.couponrush.coupon.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.couponrush.coupon.dto.CouponEventResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@DataRedisTest
class CouponEventCacheRepositoryTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private CouponEventCacheRepository repository;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        repository = new CouponEventCacheRepository(redisTemplate, objectMapper);
    }

    @Test
    void 캐시가_없으면_empty를_반환한다() {
        assertThat(repository.get()).isEmpty();
    }

    @Test
    void 저장한_목록을_그대로_복원한다() {
        List<CouponEventResponse> events = List.of(
                new CouponEventResponse(1L, "진행중 A", 3_000, 100_000,
                        LocalDateTime.of(2026, 6, 1, 0, 0), LocalDateTime.of(2026, 6, 30, 0, 0)),
                new CouponEventResponse(2L, "진행중 B", 1_000, 50_000,
                        LocalDateTime.of(2026, 6, 1, 12, 0), LocalDateTime.of(2026, 6, 10, 12, 0)));

        repository.set(events);
        Optional<List<CouponEventResponse>> result = repository.get();

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactlyElementsOf(events);
    }

    @Test
    void 저장시_TTL이_60초이하로_설정된다() {
        repository.set(List.of(new CouponEventResponse(1L, "A", 3_000, 100_000,
                LocalDateTime.of(2026, 6, 1, 0, 0), LocalDateTime.of(2026, 6, 30, 0, 0))));

        Long ttl = redisTemplate.getExpire("coupon:events:cache", TimeUnit.SECONDS);

        assertThat(ttl).isPositive().isLessThanOrEqualTo(60L);
    }
}
