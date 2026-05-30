package com.couponrush.coupon.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.couponrush.coupon.domain.CouponEvent;
import com.couponrush.coupon.redis.CouponRedisRepository;
import com.couponrush.coupon.repository.CouponEventRepository;
import com.couponrush.global.error.BusinessException;
import com.couponrush.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponStockServiceTest {

    @Mock
    private CouponEventRepository couponEventRepository;
    @Mock
    private CouponRedisRepository couponRedisRepository;
    @InjectMocks
    private CouponStockService couponStockService;

    @Test
    void 이벤트의_총재고를_Redis에_적재한다() {
        CouponEvent event = CouponEvent.builder()
                .name("진행중 이벤트")
                .totalStock(500)
                .discount(3_000)
                .startAt(LocalDateTime.of(2026, 6, 1, 0, 0))
                .endAt(LocalDateTime.of(2026, 6, 30, 0, 0))
                .build();
        given(couponEventRepository.findById(1L)).willReturn(Optional.of(event));

        couponStockService.loadStock(1L);

        verify(couponRedisRepository).setStock(1L, 500);
    }

    @Test
    void 존재하지_않는_이벤트면_예외를_던진다() {
        given(couponEventRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponStockService.loadStock(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_EVENT_NOT_FOUND);
    }
}
