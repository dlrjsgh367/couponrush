package com.couponrush.coupon.loadtest;

import com.couponrush.coupon.domain.CouponEvent;
import com.couponrush.coupon.domain.IssuedCoupon;
import com.couponrush.coupon.repository.CouponEventRepository;
import com.couponrush.coupon.repository.IssuedCouponRepository;
import com.couponrush.global.error.BusinessException;
import com.couponrush.global.error.ErrorCode;
import com.couponrush.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부하테스트 'before(개선 전)' 베이스라인. Redis 동시성 제어 없이 DB만으로 선착순을 처리한다.
 * SELECT COUNT -> INSERT 사이에 race window가 있어 동시 요청에서 재고를 초과 발급(오버셀)한다.
 * loadtest 프로파일에서만 로딩되며 MVP 발급 경로(CouponIssueService)와 완전히 분리된 스캐폴딩이다.
 */
@Service
@Profile("loadtest")
@RequiredArgsConstructor
public class NaiveCouponIssueService {

    private final CouponEventRepository couponEventRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final MemberRepository memberRepository;
    private final EntityManager em;

    @Transactional
    public void issue(Long eventId, Long memberId) {
        CouponEvent event = couponEventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_EVENT_NOT_FOUND));

        long issuedCount = em.createQuery(
                "SELECT COUNT(ic) FROM IssuedCoupon ic WHERE ic.event.id = :eventId", Long.class)
                .setParameter("eventId", eventId)
                .getSingleResult();
        if (issuedCount >= event.getTotalStock()) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }

        // 의도적 race window: 동시 트랜잭션들이 같은 count를 읽고 모두 통과 -> 재고 초과 발급
        IssuedCoupon coupon = IssuedCoupon.builder()
                .event(event)
                .member(memberRepository.getReferenceById(memberId))
                .issuedAt(LocalDateTime.now())
                .build();
        issuedCouponRepository.save(coupon);
    }
}
