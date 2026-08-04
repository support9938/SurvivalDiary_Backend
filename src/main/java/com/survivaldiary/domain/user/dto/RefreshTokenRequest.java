package com.survivaldiary.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @Schema(description = "Refresh token issued at login")
        @NotBlank(message = "Refresh token is required.")
        String refreshToken
) {
}
