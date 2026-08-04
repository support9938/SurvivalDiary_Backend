package com.survivaldiary.domain.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "정책 맞춤 추천 표시 상태")
public enum PolicyRecommendationStatus {
    RECOMMENDED,
    CHECK_REQUIRED,
    DISCOVER
}
