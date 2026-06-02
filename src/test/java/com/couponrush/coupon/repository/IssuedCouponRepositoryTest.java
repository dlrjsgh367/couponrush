package com.couponrush.coupon.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.couponrush.coupon.domain.CouponEvent;
import com.couponrush.coupon.domain.IssuedCoupon;
import com.couponrush.global.config.JpaConfig;
import com.couponrush.member.domain.Member;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class IssuedCouponRepositoryTest {

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;
    @Autowired
    private TestEntityManager em;

    @Test
    void 회원의_쿠폰을_이벤트와_함께_발급최신순으로_조회한다() {
        Member member = em.persist(member("me@couponrush.com", "나"));
        Member other = em.persist(member("other@couponrush.com", "타인"));
        CouponEvent eventA = em.persist(event("이벤트 A"));
        CouponEvent eventB = em.persist(event("이벤트 B"));

        em.persist(issuedCoupon(eventA, member, LocalDateTime.of(2026, 6, 1, 10, 0)));
        em.persist(issuedCoupon(eventB, member, LocalDateTime.of(2026, 6, 2, 10, 0)));
        em.persist(issuedCoupon(eventA, other, LocalDateTime.of(2026, 6, 3, 10, 0)));
        em.flush();
        em.clear();

        List<IssuedCoupon> result = issuedCouponRepository.findByMemberIdWithEvent(member.getId());

        // 영속성 컨텍스트를 비운 뒤에도 event 이름 접근이 가능 = JOIN FETCH로 함께 로딩됨(N+1 회피)
        assertThat(result).extracting(c -> c.getEvent().getName())
                .containsExactly("이벤트 B", "이벤트 A");
    }

    @Test
    void 발급받은_쿠폰이_없으면_빈_목록을_반환한다() {
        Member member = em.persist(member("none@couponrush.com", "무발급"));
        em.flush();
        em.clear();

        List<IssuedCoupon> result = issuedCouponRepository.findByMemberIdWithEvent(member.getId());

        assertThat(result).isEmpty();
    }

    private Member member(String email, String nickname) {
        return Member.builder()
                .email(email)
                .password("encoded-password")
                .nickname(nickname)
                .build();
    }

    private CouponEvent event(String name) {
        return CouponEvent.builder()
                .name(name)
                .totalStock(100_000)
                .discount(3_000)
                .startAt(LocalDateTime.of(2026, 6, 1, 0, 0))
                .endAt(LocalDateTime.of(2026, 6, 30, 0, 0))
                .build();
    }

    private IssuedCoupon issuedCoupon(CouponEvent event, Member member, LocalDateTime issuedAt) {
        return IssuedCoupon.builder()
                .event(event)
                .member(member)
                .issuedAt(issuedAt)
                .build();
    }
}
