package com.survivaldiary.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequest(
        @Schema(description = "카카오 또는 네이버가 앱에 발급한 액세스 토큰")
        @NotBlank String accessToken
) {
}
