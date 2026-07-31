package com.survivaldiary.domain.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "앱 정책 카테고리")
public enum PolicyCategory {
    HOUSING,
    EMPLOYMENT,
    ASSET,
    CULTURE,
    TRANSPORT
}
