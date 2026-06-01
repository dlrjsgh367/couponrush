package com.couponrush.coupon.service;

import com.couponrush.coupon.domain.IssuedCoupon;
import com.couponrush.coupon.redis.CouponRedisRepository;
import com.couponrush.coupon.repository.CouponEventRepository;
import com.couponrush.coupon.repository.IssuedCouponRepository;
import com.couponrush.global.error.BusinessException;
import com.couponrush.global.error.ErrorCode;
import com.couponrush.member.repository.MemberRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponIssueService {

    private final CouponRedisRepository couponRedisRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final CouponEventRepository couponEventRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void issue(Long eventId, Long memberId) {
        couponRedisRepository.addToQueue(eventId, memberId, System.currentTimeMillis());

        if (couponRedisRepository.isIssued(eventId, memberId)) {
            throw new BusinessException(ErrorCode.ALREADY_ISSUED);
        }

        long remaining = couponRedisRepository.decreaseStock(eventId);
        if (remaining < 0) {
            couponRedisRepository.increaseStock(eventId);
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }

        couponRedisRepository.addIssued(eventId, memberId);

        try {
            IssuedCoupon coupon = IssuedCoupon.builder()
                    .event(couponEventRepository.getReferenceById(eventId))
                    .member(memberRepository.getReferenceById(memberId))
                    .issuedAt(LocalDateTime.now())
                    .build();
            issuedCouponRepository.saveAndFlush(coupon);
        } catch (DataIntegrityViolationException e) {
            couponRedisRepository.increaseStock(eventId);
            throw new BusinessException(ErrorCode.ALREADY_ISSUED);
        } catch (RuntimeException e) {
            couponRedisRepository.increaseStock(eventId);
            couponRedisRepository.removeIssued(eventId, memberId);
            throw e;
        }
        // Kafka produce (topic=coupon-issued, key=memberId): 10번 작업에서 이 위치에 추가
    }
}
