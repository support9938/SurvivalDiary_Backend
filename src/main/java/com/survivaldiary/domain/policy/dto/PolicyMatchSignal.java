package com.survivaldiary.domain.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "정책이 사용자 조건과 일치한 근거")
public enum PolicyMatchSignal {
    AGE,
    REGION,
    DISTRICT,
    WORK_STATUS,
    JOB_SEEKING,
    EDUCATION_STATUS,
    INTEREST_EMPLOYMENT,
    INTEREST_HOUSING,
    INTEREST_EDUCATION,
    INTEREST_WELFARE_CULTURE,
    INTEREST_PARTICIPATION_RIGHTS,
    INTEREST_ASSET_BUILDING,
    INTEREST_TRANSPORT
}
