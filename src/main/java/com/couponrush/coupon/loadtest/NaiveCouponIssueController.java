package com.couponrush.coupon.loadtest;

import com.couponrush.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 부하테스트 'before' 베이스라인 엔드포인트. loadtest 프로파일에서만 노출된다.
 * POST /api/coupons/{eventId}/issue-naive
 */
@RestController
@RequestMapping("/api/coupons")
@Profile("loadtest")
@RequiredArgsConstructor
public class NaiveCouponIssueController {

    private final NaiveCouponIssueService naiveCouponIssueService;

    @PostMapping("/{eventId}/issue-naive")
    public ApiResponse<Void> issue(@PathVariable Long eventId, @AuthenticationPrincipal Long memberId) {
        naiveCouponIssueService.issue(eventId, memberId);
        return ApiResponse.success(null);
    }
}
