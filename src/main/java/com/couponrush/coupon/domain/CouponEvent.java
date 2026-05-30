package com.couponrush.coupon.domain;

import com.couponrush.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@Table(name = "coupon_events")
@Comment("쿠폰 이벤트 (마스터)")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("쿠폰 이벤트 PK")
    private Long id;

    @Column(nullable = false, length = 100)
    @Comment("이벤트명")
    private String name;

    @Column(nullable = false)
    @Comment("총 발급 수량 (불변 정의값, 실시간 재고는 Redis)")
    private int totalStock;

    @Column(nullable = false)
    @Comment("할인 금액")
    private int discount;

    @Column(nullable = false)
    @Comment("발급 시작 일시")
    private LocalDateTime startAt;

    @Column(nullable = false)
    @Comment("발급 종료 일시")
    private LocalDateTime endAt;

    @Builder
    public CouponEvent(String name, int totalStock, int discount,
                       LocalDateTime startAt, LocalDateTime endAt) {
        this.name = name;
        this.totalStock = totalStock;
        this.discount = discount;
        this.startAt = startAt;
        this.endAt = endAt;
    }
}
