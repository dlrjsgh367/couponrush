package com.couponrush.coupon.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.couponrush.coupon.domain.CouponEvent;
import com.couponrush.global.config.JpaConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class CouponEventRepositoryTest {

    @Autowired
    private CouponEventRepository couponEventRepository;

    @Test
    void 발급_기간_중인_이벤트만_시작일_오름차순으로_조회한다() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);

        couponEventRepository.save(event("진행중 B", now.minusDays(1), now.plusDays(1)));
        couponEventRepository.save(event("진행중 A", now.minusDays(3), now.plusDays(2)));
        couponEventRepository.save(event("아직 시작 전", now.plusDays(1), now.plusDays(5)));
        couponEventRepository.save(event("이미 종료", now.minusDays(5), now.minusDays(1)));

        List<CouponEvent> result = couponEventRepository.findIssuableEvents(now);

        assertThat(result).extracting(CouponEvent::getName)
                .containsExactly("진행중 A", "진행중 B");
    }

    @Test
    void 경계값_시작일과_종료일이_now와_같으면_포함된다() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);

        couponEventRepository.save(event("시작일이 now", now, now.plusDays(1)));
        couponEventRepository.save(event("종료일이 now", now.minusDays(1), now));

        List<CouponEvent> result = couponEventRepository.findIssuableEvents(now);

        assertThat(result).extracting(CouponEvent::getName)
                .containsExactlyInAnyOrder("시작일이 now", "종료일이 now");
    }

    private CouponEvent event(String name, LocalDateTime startAt, LocalDateTime endAt) {
        return CouponEvent.builder()
                .name(name)
                .totalStock(100_000)
                .discount(3_000)
                .startAt(startAt)
                .endAt(endAt)
                .build();
    }
}
