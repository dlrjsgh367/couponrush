package com.couponrush.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.couponrush.coupon.domain.CouponEvent;
import com.couponrush.coupon.domain.IssuedCoupon;
import com.couponrush.coupon.dto.MyCouponResponse;
import com.couponrush.coupon.kafka.CouponProducer;
import com.couponrush.coupon.kafka.dto.CouponIssuedEvent;
import com.couponrush.coupon.redis.CouponRedisRepository;
import com.couponrush.coupon.repository.CouponEventRepository;
import com.couponrush.coupon.repository.IssuedCouponRepository;
import com.couponrush.global.error.BusinessException;
import com.couponrush.global.error.ErrorCode;
import com.couponrush.member.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class CouponIssueServiceTest {

    private static final Long EVENT_ID = 1L;
    private static final Long MEMBER_ID = 100L;

    @Mock
    private CouponRedisRepository couponRedisRepository;
    @Mock
    private IssuedCouponRepository issuedCouponRepository;
    @Mock
    private CouponEventRepository couponEventRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CouponProducer couponProducer;
    @InjectMocks
    private CouponIssueService couponIssueService;

    @Test
    void 재고가_있으면_대기열_등록후_발급에_성공한다() {
        given(couponRedisRepository.isIssued(EVENT_ID, MEMBER_ID)).willReturn(false);
        given(couponRedisRepository.decreaseStock(EVENT_ID)).willReturn(0L);

        assertThatCode(() -> couponIssueService.issue(EVENT_ID, MEMBER_ID)).doesNotThrowAnyException();

        verify(couponRedisRepository).addToQueue(eq(EVENT_ID), eq(MEMBER_ID), anyLong());
        verify(couponRedisRepository).addIssued(EVENT_ID, MEMBER_ID);
        verify(issuedCouponRepository).saveAndFlush(any(IssuedCoupon.class));
        verify(couponProducer).produce(any(CouponIssuedEvent.class));
        verify(couponRedisRepository, never()).increaseStock(EVENT_ID);
        verify(couponRedisRepository, never()).removeIssued(EVENT_ID, MEMBER_ID);
    }

    @Test
    void 이미_발급받았으면_재고를_건드리지_않고_거절한다() {
        given(couponRedisRepository.isIssued(EVENT_ID, MEMBER_ID)).willReturn(true);

        assertThatThrownBy(() -> couponIssueService.issue(EVENT_ID, MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_ISSUED);

        verify(couponRedisRepository, never()).decreaseStock(EVENT_ID);
        verify(issuedCouponRepository, never()).saveAndFlush(any());
        verify(couponProducer, never()).produce(any(CouponIssuedEvent.class));
    }

    @Test
    void 재고가_소진되면_과차감을_복구하고_거절한다() {
        given(couponRedisRepository.isIssued(EVENT_ID, MEMBER_ID)).willReturn(false);
        given(couponRedisRepository.decreaseStock(EVENT_ID)).willReturn(-1L);

        assertThatThrownBy(() -> couponIssueService.issue(EVENT_ID, MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.OUT_OF_STOCK);

        verify(couponRedisRepository).increaseStock(EVENT_ID);
        verify(couponRedisRepository, never()).addIssued(EVENT_ID, MEMBER_ID);
        verify(issuedCouponRepository, never()).saveAndFlush(any());
        verify(couponProducer, never()).produce(any(CouponIssuedEvent.class));
    }

    @Test
    void 유니크제약_위반이면_재고를_복구하고_중복으로_거절한다() {
        given(couponRedisRepository.isIssued(EVENT_ID, MEMBER_ID)).willReturn(false);
        given(couponRedisRepository.decreaseStock(EVENT_ID)).willReturn(0L);
        given(issuedCouponRepository.saveAndFlush(any(IssuedCoupon.class)))
                .willThrow(new DataIntegrityViolationException("uk_issued_coupons_event_member"));

        assertThatThrownBy(() -> couponIssueService.issue(EVENT_ID, MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_ISSUED);

        verify(couponRedisRepository).increaseStock(EVENT_ID);
        verify(couponRedisRepository, never()).removeIssued(EVENT_ID, MEMBER_ID);
        verify(couponProducer, never()).produce(any(CouponIssuedEvent.class));
    }

    @Test
    void INSERT가_예기치않게_실패하면_재고와_발급자등록을_모두_복구한다() {
        given(couponRedisRepository.isIssued(EVENT_ID, MEMBER_ID)).willReturn(false);
        given(couponRedisRepository.decreaseStock(EVENT_ID)).willReturn(0L);
        given(issuedCouponRepository.saveAndFlush(any(IssuedCoupon.class)))
                .willThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> couponIssueService.issue(EVENT_ID, MEMBER_ID))
                .isInstanceOf(IllegalStateException.class);

        verify(couponRedisRepository).increaseStock(EVENT_ID);
        verify(couponRedisRepository).removeIssued(EVENT_ID, MEMBER_ID);
        verify(couponProducer, never()).produce(any(CouponIssuedEvent.class));
    }

    @Test
    void 내_쿠폰을_조회하면_이벤트정보를_매핑해_반환한다() {
        CouponEvent event = CouponEvent.builder()
                .name("3천원 할인")
                .totalStock(100_000)
                .discount(3_000)
                .startAt(LocalDateTime.of(2026, 6, 1, 0, 0))
                .endAt(LocalDateTime.of(2026, 6, 30, 0, 0))
                .build();
        IssuedCoupon coupon = IssuedCoupon.builder()
                .event(event)
                .issuedAt(LocalDateTime.of(2026, 6, 2, 10, 0))
                .build();
        given(issuedCouponRepository.findByMemberIdWithEvent(MEMBER_ID)).willReturn(List.of(coupon));

        List<MyCouponResponse> result = couponIssueService.getMyCoupons(MEMBER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).eventName()).isEqualTo("3천원 할인");
        assertThat(result.get(0).discount()).isEqualTo(3_000);
        assertThat(result.get(0).endAt()).isEqualTo(LocalDateTime.of(2026, 6, 30, 0, 0));
        assertThat(result.get(0).issuedAt()).isEqualTo(LocalDateTime.of(2026, 6, 2, 10, 0));
    }
}
