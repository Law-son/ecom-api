package com.eyarko.ecom.util;

import com.eyarko.ecom.dto.ApiResponse;

public final class ResponseUtil {
    private ResponseUtil() {
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
            .status("success")
            .message(message)
            .data(data)
            .build();
    }
}


