package com.couponrush.coupon.service;

import com.couponrush.coupon.dto.CouponEventResponse;
import com.couponrush.coupon.redis.CouponEventCacheRepository;
import com.couponrush.coupon.repository.CouponEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponEventService {

    private final CouponEventRepository couponEventRepository;
    private final CouponEventCacheRepository couponEventCacheRepository;

    public List<CouponEventResponse> getIssuableEvents() {
        return couponEventCacheRepository.get()
                .orElseGet(this::loadFromDbAndCache);
    }

    private List<CouponEventResponse> loadFromDbAndCache() {
        List<CouponEventResponse> events = couponEventRepository.findIssuableEvents(LocalDateTime.now())
                .stream()
                .map(CouponEventResponse::from)
                .toList();
        couponEventCacheRepository.set(events);
        return events;
    }
}
