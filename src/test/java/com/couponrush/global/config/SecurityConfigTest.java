package com.couponrush.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.couponrush.coupon.controller.CouponEventController;
import com.couponrush.coupon.service.CouponEventService;
import com.couponrush.global.jwt.JwtProvider;
import com.couponrush.global.jwt.TokenBlacklistRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CouponEventController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CouponEventService couponEventService;
    @MockitoBean
    private JwtProvider jwtProvider;
    @MockitoBean
    private TokenBlacklistRepository tokenBlacklistRepository;

    @Test
    void 인증없이_보호된_경로를_호출하면_401과_에러본문을_반환한다() throws Exception {
        mockMvc.perform(get("/api/coupons/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C003"));
    }
}
