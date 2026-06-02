package com.couponrush.coupon.repository;

import com.couponrush.coupon.domain.IssuedCoupon;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IssuedCouponRepository extends JpaRepository<IssuedCoupon, Long> {

    @Query("SELECT ic FROM IssuedCoupon ic JOIN FETCH ic.event WHERE ic.member.id = :memberId ORDER BY ic.issuedAt DESC")
    List<IssuedCoupon> findByMemberIdWithEvent(@Param("memberId") Long memberId);
}
