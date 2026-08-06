package com.survivaldiary.domain.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "제공처 신청 기간의 안전한 분류")
public enum PolicyApplicationPeriodType {
    FIXED,
    ALWAYS,
    CLOSED,
    UNTIL_BUDGET,
    UNKNOWN
}
