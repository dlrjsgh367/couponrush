package com.couponrush.coupon.repository;

import com.couponrush.coupon.domain.CouponEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponEventRepository extends JpaRepository<CouponEvent, Long> {
}
