package com.couponrush.coupon.redis;

import com.couponrush.coupon.dto.CouponEventResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CouponEventCacheRepository {

    private static final String CACHE_KEY = "coupon:events:cache";
    private static final Duration TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<List<CouponEventResponse>> get() {
        String json = redisTemplate.opsForValue().get(CACHE_KEY);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, new TypeReference<>() {
            }));
        } catch (JsonProcessingException e) {
            log.warn("쿠폰 이벤트 캐시 역직렬화 실패, miss로 처리한다", e);
            return Optional.empty();
        }
    }

    public void set(List<CouponEventResponse> events) {
        try {
            String json = objectMapper.writeValueAsString(events);
            redisTemplate.opsForValue().set(CACHE_KEY, json, TTL);
        } catch (JsonProcessingException e) {
            log.warn("쿠폰 이벤트 캐시 적재 실패, 캐시 없이 진행한다", e);
        }
    }
}
