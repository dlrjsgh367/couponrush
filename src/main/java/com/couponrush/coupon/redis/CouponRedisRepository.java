package com.couponrush.coupon.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CouponRedisRepository {

    private static final String QUEUE_KEY = "coupon:queue:";
    private static final String ISSUED_KEY = "coupon:issued:";
    private static final String STOCK_KEY = "coupon:stock:";

    private final StringRedisTemplate redisTemplate;

    public void addToQueue(Long eventId, Long memberId, long timestamp) {
        redisTemplate.opsForZSet().add(QUEUE_KEY + eventId, String.valueOf(memberId), timestamp);
    }

    public boolean isIssued(Long eventId, Long memberId) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(ISSUED_KEY + eventId, String.valueOf(memberId)));
    }

    public long decreaseStock(Long eventId) {
        return redisTemplate.opsForValue().decrement(STOCK_KEY + eventId);
    }

    public long increaseStock(Long eventId) {
        return redisTemplate.opsForValue().increment(STOCK_KEY + eventId);
    }

    public void addIssued(Long eventId, Long memberId) {
        redisTemplate.opsForSet().add(ISSUED_KEY + eventId, String.valueOf(memberId));
    }

    public void removeIssued(Long eventId, Long memberId) {
        redisTemplate.opsForSet().remove(ISSUED_KEY + eventId, String.valueOf(memberId));
    }

    public void setStock(Long eventId, int stock) {
        redisTemplate.opsForValue().set(STOCK_KEY + eventId, String.valueOf(stock));
    }
}
