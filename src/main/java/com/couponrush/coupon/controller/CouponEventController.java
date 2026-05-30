package com.couponrush.coupon.controller;

import com.couponrush.coupon.dto.CouponEventResponse;
import com.couponrush.coupon.service.CouponEventService;
import com.couponrush.global.common.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponEventController {

    private final CouponEventService couponEventService;

    @GetMapping("/events")
    public ApiResponse<List<CouponEventResponse>> getEvents() {
        return ApiResponse.success(couponEventService.getIssuableEvents());
    }
}
