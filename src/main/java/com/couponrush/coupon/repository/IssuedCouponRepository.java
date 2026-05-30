package com.couponrush.coupon.repository;

import com.couponrush.coupon.domain.IssuedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssuedCouponRepository extends JpaRepository<IssuedCoupon, Long> {
}
