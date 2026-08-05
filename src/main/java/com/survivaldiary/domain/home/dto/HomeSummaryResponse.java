package com.survivaldiary.domain.home.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "홈 화면 예산 및 지출 요약")
public record HomeSummaryResponse(
        String userName,
        long dailyLimit,
        long remainingToday,
        long spentToday,
        long savedToday,
        long weeklyBudget,
        long weeklySpent,
        Long topCategoryId
) {
}
