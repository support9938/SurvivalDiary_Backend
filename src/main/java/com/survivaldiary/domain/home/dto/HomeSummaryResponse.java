package com.survivaldiary.domain.home.dto;

public record HomeSummaryResponse(
        String userName,
        int dailyLimit,
        int remainingToday,
        int spentToday,
        int savedToday,
        int weeklyBudget,
        int weeklySpent
) {}
