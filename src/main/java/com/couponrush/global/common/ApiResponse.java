package com.couponrush.global.common;

import com.couponrush.global.error.ErrorCode;

public record ApiResponse<T>(boolean success, T data, ApiError error) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, new ApiError(errorCode.getCode(), errorCode.getMessage()));
    }

    public record ApiError(String code, String message) {

    }
}
