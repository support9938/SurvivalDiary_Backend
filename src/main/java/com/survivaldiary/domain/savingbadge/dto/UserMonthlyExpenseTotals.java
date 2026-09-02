package com.survivaldiary.domain.savingbadge.dto;

public record UserMonthlyExpenseTotals(
        Long userId,
        Long olderMonthAmount,
        Long recentMonthAmount,
        Long olderMonthCount,
        Long recentMonthCount
) {
}
