package com.survivaldiary.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(

        @Schema(description = "액세스 토큰 (Authorization: Bearer {token})")
        String accessToken,

        @Schema(description = "리프레시 토큰 — 액세스 토큰 만료 시 재발급에 사용")
        String refreshToken,

        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        @Schema(description = "액세스 토큰 유효 시간(초)", example = "3600")
        long expiresInSeconds
) {

    public static TokenResponse of(String accessToken, String refreshToken,
                                   long accessTokenValidityMs) {
        return new TokenResponse(accessToken, refreshToken, "Bearer",
                accessTokenValidityMs / 1000);
    }
}
