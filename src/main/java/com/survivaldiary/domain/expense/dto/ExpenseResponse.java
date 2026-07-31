package com.survivaldiary.domain.expense.dto;

import com.survivaldiary.domain.expense.entity.Expense;
import java.time.LocalDateTime;

public record ExpenseResponse(
        Long expenseId,
        Long userId,
        Long categoryId,
        String title,
        Integer amount,
        LocalDateTime spentAt,
        String memo,
        Expense.EntryType entryType,
        LocalDateTime createdAt
) {

    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getUserId(),
                expense.getCategoryId(),
                expense.getTitle(),
                expense.getAmount(),
                expense.getSpentAt(),
                expense.getMemo(),
                expense.getEntryType(),
                expense.getCreatedAt()
        );
    }
}
