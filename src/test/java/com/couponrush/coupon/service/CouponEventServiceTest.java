package com.couponrush.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.couponrush.coupon.domain.CouponEvent;
import com.couponrush.coupon.dto.CouponEventResponse;
import com.couponrush.coupon.redis.CouponEventCacheRepository;
import com.couponrush.coupon.repository.CouponEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponEventServiceTest {

    @Mock
    private CouponEventRepository couponEventRepository;
    @Mock
    private CouponEventCacheRepository couponEventCacheRepository;
    @InjectMocks
    private CouponEventService couponEventService;

    @Test
    void 캐시가_있으면_캐시를_반환하고_DB는_조회하지_않는다() {
        List<CouponEventResponse> cached = List.of(
                new CouponEventResponse(1L, "캐시된 이벤트", 3_000, 100_000,
                        LocalDateTime.of(2026, 6, 1, 0, 0), LocalDateTime.of(2026, 6, 30, 0, 0)));
        given(couponEventCacheRepository.get()).willReturn(Optional.of(cached));

        List<CouponEventResponse> result = couponEventService.getIssuableEvents();

        assertThat(result).isEqualTo(cached);
        verify(couponEventRepository, never()).findIssuableEvents(any());
        verify(couponEventCacheRepository, never()).set(any());
    }

    @Test
    void 캐시가_없으면_DB를_조회하고_캐시에_적재한다() {
        given(couponEventCacheRepository.get()).willReturn(Optional.empty());
        given(couponEventRepository.findIssuableEvents(any())).willReturn(List.of(
                CouponEvent.builder()
                        .name("진행중 이벤트")
                        .totalStock(100_000)
                        .discount(3_000)
                        .startAt(LocalDateTime.of(2026, 6, 1, 0, 0))
                        .endAt(LocalDateTime.of(2026, 6, 30, 0, 0))
                        .build()));

        List<CouponEventResponse> result = couponEventService.getIssuableEvents();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("진행중 이벤트");
        verify(couponEventCacheRepository).set(result);
    }
}
