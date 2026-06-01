package com.couponrush.coupon.kafka.dto;

import java.time.LocalDateTime;

public record CouponIssuedEvent(Long eventId, Long memberId, LocalDateTime issuedAt) {
}
