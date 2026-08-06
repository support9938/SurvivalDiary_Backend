package com.survivaldiary.domain.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공식 신청 URL의 이동 성격")
public enum PolicyOfficialLinkType {
    APPLICATION_CANDIDATE,
    LOGIN_REQUIRED,
    INSTITUTION_HOME,
    UNKNOWN,
    UNAVAILABLE
}
