package com.survivaldiary.domain.budget.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BudgetAmountRequest(
        @NotNull(message = "예산 금액은 필수입니다.")
        @Min(value = 1, message = "예산 금액은 1원 이상이어야 합니다.")
        @Max(value = 1_000_000_000, message = "예산 금액은 10억원 이하여야 합니다.")
        Integer amount
) {
}
