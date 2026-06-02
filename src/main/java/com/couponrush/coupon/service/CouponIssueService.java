package com.couponrush.coupon.service;

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
    private final CouponProducer couponProducer;

    @Transactional
    public void issue(Long eventId, Long memberId) {
        LocalDateTime issuedAt = LocalDateTime.now();
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
                    .issuedAt(issuedAt)
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
        couponProducer.produce(new CouponIssuedEvent(eventId, memberId, issuedAt));
    }

    @Transactional(readOnly = true)
    public List<MyCouponResponse> getMyCoupons(Long memberId) {
        return issuedCouponRepository.findByMemberIdWithEvent(memberId).stream()
                .map(MyCouponResponse::from)
                .toList();
    }
}
