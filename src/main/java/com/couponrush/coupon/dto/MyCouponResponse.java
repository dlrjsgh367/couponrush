package com.couponrush.coupon.dto;

import com.couponrush.coupon.domain.CouponEvent;
import com.couponrush.coupon.domain.IssuedCoupon;
import java.time.LocalDateTime;

public record MyCouponResponse(
    Long couponId,
    Long eventId,
    String eventName,
    int discount,
    LocalDateTime endAt,
    LocalDateTime issuedAt
) {

    public static MyCouponResponse from(IssuedCoupon coupon) {
        CouponEvent event = coupon.getEvent();
        return new MyCouponResponse(
            coupon.getId(),
            event.getId(),
            event.getName(),
            event.getDiscount(),
            event.getEndAt(),
            coupon.getIssuedAt()
        );
    }
}
