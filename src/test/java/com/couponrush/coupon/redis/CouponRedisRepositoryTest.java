package com.couponrush.coupon.redis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@DataRedisTest
class CouponRedisRepositoryTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private CouponRedisRepository repository;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        repository = new CouponRedisRepository(redisTemplate);
    }

    @Test
    void 재고를_적재하고_차감하면_순서대로_감소한다() {
        repository.setStock(1L, 3);

        assertThat(repository.decreaseStock(1L)).isEqualTo(2L);
        assertThat(repository.decreaseStock(1L)).isEqualTo(1L);
        assertThat(repository.decreaseStock(1L)).isEqualTo(0L);
        assertThat(repository.decreaseStock(1L)).isEqualTo(-1L);
    }

    @Test
    void 차감한_재고를_다시_증가시키면_원복된다() {
        repository.setStock(1L, 1);
        repository.decreaseStock(1L);

        assertThat(repository.increaseStock(1L)).isEqualTo(1L);
    }

    @Test
    void 발급자를_등록하면_isIssued가_참이되고_미등록은_거짓이다() {
        repository.addIssued(1L, 100L);

        assertThat(repository.isIssued(1L, 100L)).isTrue();
        assertThat(repository.isIssued(1L, 200L)).isFalse();
    }

    @Test
    void 발급자를_제거하면_isIssued가_다시_거짓이된다() {
        repository.addIssued(1L, 100L);
        repository.removeIssued(1L, 100L);

        assertThat(repository.isIssued(1L, 100L)).isFalse();
    }

    @Test
    void 대기열에_추가하면_score와_함께_등록된다() {
        repository.addToQueue(1L, 100L, 1_700_000_000L);

        Double score = redisTemplate.opsForZSet().score("coupon:queue:1", "100");
        Long size = redisTemplate.opsForZSet().size("coupon:queue:1");
        assertThat(score).isEqualTo(1_700_000_000d);
        assertThat(size).isEqualTo(1L);
    }
}
