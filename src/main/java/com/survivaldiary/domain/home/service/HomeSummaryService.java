package com.survivaldiary.domain.home.service;

import com.survivaldiary.domain.budget.repository.BudgetRepository;
import com.survivaldiary.domain.expense.repository.ExpenseRepository;
import com.survivaldiary.domain.home.dto.HomeSummaryResponse;
import com.survivaldiary.domain.user.entity.User;
import com.survivaldiary.domain.user.repository.UserRepository;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeSummaryService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public HomeSummaryResponse getSummary(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate tomorrow = today.plusDays(1);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate nextWeekStart = weekStart.plusWeeks(1);

        long dailyLimit = budgetRepository
                .findFirstByUserIdAndBudgetDateLessThanEqualOrderByBudgetDateDesc(userId, today)
                .map(budget -> budget.getAmount().longValue())
                .orElse(0L);
        long spentToday = expenseRepository.sumAmountByUserIdAndPeriod(
                userId,
                today.atStartOfDay(),
                tomorrow.atStartOfDay()
        );
        long weeklyBudget = dailyLimit * 7L;
        long weeklySpent = expenseRepository.sumAmountByUserIdAndPeriod(
                userId,
                weekStart.atStartOfDay(),
                nextWeekStart.atStartOfDay()
        );
        Long topCategoryId = expenseRepository.findCategoryIdsBySpendDescending(
                        userId,
                        today.atStartOfDay(),
                        tomorrow.atStartOfDay()
                ).stream()
                .findFirst()
                .orElse(null);

        return new HomeSummaryResponse(
                user.getName(),
                dailyLimit,
                Math.max(dailyLimit - spentToday, 0L),
                spentToday,
                0L,
                weeklyBudget,
                weeklySpent,
                topCategoryId
        );
    }
}
