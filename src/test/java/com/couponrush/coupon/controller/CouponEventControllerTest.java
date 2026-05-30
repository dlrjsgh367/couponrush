package com.couponrush.coupon.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.couponrush.coupon.dto.CouponEventResponse;
import com.couponrush.coupon.service.CouponEventService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CouponEventController.class)
@AutoConfigureMockMvc(addFilters = false)
class CouponEventControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CouponEventService couponEventService;

    @Test
    void 이벤트_목록을_success로_반환한다() throws Exception {
        given(couponEventService.getIssuableEvents()).willReturn(List.of(
                new CouponEventResponse(1L, "진행중 이벤트", 3_000, 100_000,
                        LocalDateTime.of(2026, 6, 1, 0, 0), LocalDateTime.of(2026, 6, 30, 0, 0))));

        mockMvc.perform(get("/api/coupons/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("진행중 이벤트"))
                .andExpect(jsonPath("$.data[0].discount").value(3_000));
    }
}
