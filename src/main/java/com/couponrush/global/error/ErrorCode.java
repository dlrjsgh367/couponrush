package com.couponrush.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 오류가 발생했습니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C003", "인증이 필요합니다"),

    // 회원
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "M001", "이미 가입된 이메일입니다"),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M002", "회원을 찾을 수 없습니다"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "M003", "이메일 또는 비밀번호가 올바르지 않습니다"),

    // 쿠폰
    COUPON_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CP001", "쿠폰 이벤트를 찾을 수 없습니다"),
    ALREADY_ISSUED(HttpStatus.CONFLICT, "CP002", "이미 발급받은 쿠폰입니다"),
    OUT_OF_STOCK(HttpStatus.CONFLICT, "CP003", "쿠폰이 모두 소진되었습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
