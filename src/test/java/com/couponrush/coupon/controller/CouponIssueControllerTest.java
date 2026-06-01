package com.couponrush.coupon.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.couponrush.coupon.service.CouponIssueService;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CouponIssueController.class)
@AutoConfigureMockMvc(addFilters = false)
class CouponIssueControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CouponIssueService couponIssueService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 발급_성공시_200과_success를_반환하고_인증회원으로_발급한다() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(100L, null, Collections.emptyList()));

        mockMvc.perform(post("/api/coupons/1/issue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(couponIssueService).issue(1L, 100L);
    }
}
