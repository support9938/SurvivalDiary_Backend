package com.survivaldiary.domain.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지원 금액의 지급 단위와 최대 금액 여부")
public enum PolicySupportAmountType {
    FIXED,
    MAXIMUM,
    MONTHLY,
    MONTHLY_MAXIMUM
}
