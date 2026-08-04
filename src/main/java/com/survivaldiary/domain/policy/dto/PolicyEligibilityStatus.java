package com.survivaldiary.domain.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "입력 조건과 정책의 일치 판정 상태")
public enum PolicyEligibilityStatus {
    MATCHED,
    CHECK_REQUIRED
}
