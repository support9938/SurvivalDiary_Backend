package com.survivaldiary.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.survivaldiary.global.exception.ErrorCode;

/**
 * 전 API 공통 응답 포맷.
 *
 * 성공: { "success": true,  "data": {...} }
 * 실패: { "success": false, "error": { "code": "U001", "message": "..." } }
 *
 * 모든 도메인 컨트롤러는 이 포맷으로 응답한다 — Flutter 파싱 코드 공유를 위한 팀 규약.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ErrorBody error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(errorCode.getCode(), message));
    }

    public record ErrorBody(String code, String message) {}
}
