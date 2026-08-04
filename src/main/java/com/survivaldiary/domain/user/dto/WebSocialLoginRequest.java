package com.survivaldiary.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record WebSocialLoginRequest(
        @NotBlank String authorizationCode,
        @NotBlank String redirectUri,
        String state
) {
}
