package com.couponrush.coupon.repository;

import com.couponrush.coupon.domain.CouponEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponEventRepository extends JpaRepository<CouponEvent, Long> {

    @Query("SELECT e FROM CouponEvent e WHERE e.startAt <= :now AND e.endAt >= :now ORDER BY e.startAt ASC")
    List<CouponEvent> findIssuableEvents(@Param("now") LocalDateTime now);
}
