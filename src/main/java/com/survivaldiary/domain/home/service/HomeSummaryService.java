package com.survivaldiary.domain.home.service;

import com.survivaldiary.domain.budget.entity.Budget;
import com.survivaldiary.domain.budget.repository.BudgetRepository;
import com.survivaldiary.domain.expense.entity.Expense;
import com.survivaldiary.domain.expense.repository.ExpenseRepository;
import com.survivaldiary.domain.home.dto.HomeSummaryResponse;
import com.survivaldiary.domain.user.entity.User;
import com.survivaldiary.domain.user.repository.UserRepository;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeSummaryService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public HomeSummaryResponse getSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalDate today = LocalDate.now(SEOUL);
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        int dailyLimit = budgetRepository.findByUserIdAndBudgetDate(userId, today)
                .map(Budget::getAmount)
                .orElse(0);
        int weeklyBudget = budgetRepository
                .findAllByUserIdAndBudgetDateBetween(userId, weekStart, weekEnd)
                .stream()
                .mapToInt(Budget::getAmount)
                .sum();

        int spentToday = sumExpenses(userId, today, today.plusDays(1));
        int weeklySpent = sumExpenses(userId, weekStart, weekEnd.plusDays(1));

        return new HomeSummaryResponse(
                user.getName(),
                dailyLimit,
                Math.max(dailyLimit - spentToday, 0),
                spentToday,
                Math.max(dailyLimit - spentToday, 0),
                weeklyBudget,
                weeklySpent
        );
    }

    private int sumExpenses(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atStartOfDay();
        return expenseRepository.findAllByUserIdAndSpentAtGreaterThanEqualAndSpentAtLessThan(
                        userId, start, end
                ).stream()
                .mapToInt(Expense::getAmount)
                .sum();
    }
}
