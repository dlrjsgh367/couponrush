package com.couponrush.global.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private static final String SECRET = "couponrush-test-secret-key-must-be-at-least-256-bits-long";
    private static final long ONE_HOUR = 3600_000L;

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, ONE_HOUR);

    @Test
    void 발급한_토큰은_유효하다() {
        String token = jwtProvider.createToken(1L, "user@test.com");

        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    void 토큰에서_회원ID를_추출한다() {
        String token = jwtProvider.createToken(42L, "user@test.com");

        assertThat(jwtProvider.getMemberId(token)).isEqualTo(42L);
    }

    @Test
    void 다른_secret으로_서명된_토큰은_유효하지_않다() {
        JwtProvider other = new JwtProvider("a-completely-different-secret-key-also-256-bits-long-enough", ONE_HOUR);
        String foreignToken = other.createToken(1L, "user@test.com");

        assertThat(jwtProvider.validateToken(foreignToken)).isFalse();
    }

    @Test
    void 형식이_틀린_토큰은_유효하지_않다() {
        assertThat(jwtProvider.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    void 만료된_토큰은_유효하지_않다() throws InterruptedException {
        JwtProvider shortLived = new JwtProvider(SECRET, 1L);
        String token = shortLived.createToken(1L, "user@test.com");
        Thread.sleep(10L);

        assertThat(shortLived.validateToken(token)).isFalse();
    }

    @Test
    void 토큰의_잔여_만료시간을_반환한다() {
        String token = jwtProvider.createToken(1L, "user@test.com");

        long remaining = jwtProvider.getRemainingMillis(token);

        assertThat(remaining).isPositive().isLessThanOrEqualTo(ONE_HOUR);
        assertThat(remaining).isGreaterThan(ONE_HOUR - 5_000L);
    }
}
