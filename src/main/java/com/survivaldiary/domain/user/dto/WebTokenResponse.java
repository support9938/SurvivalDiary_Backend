package com.survivaldiary.domain.user.dto;

public record WebTokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
    public static WebTokenResponse from(TokenResponse tokens) {
        return new WebTokenResponse(
                tokens.accessToken(),
                tokens.tokenType(),
                tokens.expiresInSeconds()
        );
    }
}
