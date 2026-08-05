package com.survivaldiary.domain.budget.dto;

import java.time.LocalDate;

public record BudgetResponse(
        LocalDate budgetDate,
        int amount,
        boolean saved
) {
    public static BudgetResponse empty(LocalDate date) {
        return new BudgetResponse(date, 0, false);
    }
}
