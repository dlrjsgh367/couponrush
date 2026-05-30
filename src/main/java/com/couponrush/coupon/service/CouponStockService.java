package com.couponrush.coupon.service;

import com.couponrush.coupon.domain.CouponEvent;
import com.couponrush.coupon.redis.CouponRedisRepository;
import com.couponrush.coupon.repository.CouponEventRepository;
import com.couponrush.global.error.BusinessException;
import com.couponrush.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponStockService {

    private final CouponEventRepository couponEventRepository;
    private final CouponRedisRepository couponRedisRepository;

    @Transactional(readOnly = true)
    public void loadStock(Long eventId) {
        CouponEvent event = couponEventRepository.findById(eventId)
            .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_EVENT_NOT_FOUND));
        couponRedisRepository.setStock(eventId, event.getTotalStock());
    }
}
