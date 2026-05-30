package com.couponrush.coupon.domain;

import com.couponrush.global.common.BaseTimeEntity;
import com.couponrush.member.domain.Member;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@Table(name = "issued_coupons")
@Comment("발급된 쿠폰 (중복 발급 방지: event_id+member_id 유니크)")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssuedCoupon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("발급 쿠폰 PK")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @Comment("쿠폰 이벤트 FK")
    private CouponEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @Comment("발급받은 회원 FK")
    private Member member;

    @Comment("발급 일시")
    private LocalDateTime issuedAt;

    @Builder
    public IssuedCoupon(CouponEvent event, Member member, LocalDateTime issuedAt) {
        this.event = event;
        this.member = member;
        this.issuedAt = issuedAt;
    }
}
