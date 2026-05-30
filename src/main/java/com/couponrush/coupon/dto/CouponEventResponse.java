package com.couponrush.coupon.dto;

import com.couponrush.coupon.domain.CouponEvent;
import java.time.LocalDateTime;

public record CouponEventResponse(
    Long id,
    String name,
    int discount,
    int totalStock,
    LocalDateTime startAt,
    LocalDateTime endAt
) {

    public static CouponEventResponse from(CouponEvent event) {
        return new CouponEventResponse(
            event.getId(),
            event.getName(),
            event.getDiscount(),
            event.getTotalStock(),
            event.getStartAt(),
            event.getEndAt()
        );
    }
}
