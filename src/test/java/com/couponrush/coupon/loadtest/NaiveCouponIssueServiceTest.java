package com.couponrush.coupon.loadtest;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.couponrush.coupon.domain.CouponEvent;
import com.couponrush.coupon.domain.IssuedCoupon;
import com.couponrush.coupon.repository.CouponEventRepository;
import com.couponrush.coupon.repository.IssuedCouponRepository;
import com.couponrush.global.error.BusinessException;
import com.couponrush.global.error.ErrorCode;
import com.couponrush.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NaiveCouponIssueServiceTest {

    private static final Long EVENT_ID = 1L;
    private static final Long MEMBER_ID = 100L;

    @Mock
    private CouponEventRepository couponEventRepository;
    @Mock
    private IssuedCouponRepository issuedCouponRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private EntityManager em;
    @Mock
    private TypedQuery<Long> countQuery;
    @InjectMocks
    private NaiveCouponIssueService naiveCouponIssueService;

    private void givenIssuedCount(long count) {
        given(em.createQuery(anyString(), eq(Long.class))).willReturn(countQuery);
        given(countQuery.setParameter(eq("eventId"), any())).willReturn(countQuery);
        given(countQuery.getSingleResult()).willReturn(count);
    }

    private CouponEvent event(int totalStock) {
        return CouponEvent.builder()
                .name("부하테스트 이벤트")
                .totalStock(totalStock)
                .discount(3_000)
                .startAt(LocalDateTime.of(2026, 6, 1, 0, 0))
                .endAt(LocalDateTime.of(2026, 6, 30, 0, 0))
                .build();
    }

    @Test
    void 발급수가_재고보다_적으면_발급에_성공한다() {
        given(couponEventRepository.findById(EVENT_ID)).willReturn(Optional.of(event(100)));
        givenIssuedCount(99);

        assertThatCode(() -> naiveCouponIssueService.issue(EVENT_ID, MEMBER_ID)).doesNotThrowAnyException();

        verify(issuedCouponRepository).save(any(IssuedCoupon.class));
    }

    @Test
    void 발급수가_재고에_도달하면_품절로_거절한다() {
        given(couponEventRepository.findById(EVENT_ID)).willReturn(Optional.of(event(100)));
        givenIssuedCount(100);

        assertThatThrownBy(() -> naiveCouponIssueService.issue(EVENT_ID, MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.OUT_OF_STOCK);

        verify(issuedCouponRepository, never()).save(any());
    }
}
