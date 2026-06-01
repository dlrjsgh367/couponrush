package com.couponrush.coupon.controller;

import com.couponrush.coupon.service.CouponIssueService;
import com.couponrush.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponIssueController {

    private final CouponIssueService couponIssueService;

    @PostMapping("/{eventId}/issue")
    public ApiResponse<Void> issue(@PathVariable Long eventId, @AuthenticationPrincipal Long memberId) {
        couponIssueService.issue(eventId, memberId);
        return ApiResponse.success(null);
    }
}
