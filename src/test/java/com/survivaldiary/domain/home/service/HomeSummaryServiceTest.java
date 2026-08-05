package com.survivaldiary.domain.home.service;

import com.survivaldiary.domain.budget.entity.Budget;
import com.survivaldiary.domain.budget.repository.BudgetRepository;
import com.survivaldiary.domain.expense.repository.ExpenseRepository;
import com.survivaldiary.domain.user.entity.User;
import com.survivaldiary.domain.user.repository.UserRepository;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeSummaryServiceTest {

    private UserRepository userRepository;
    private BudgetRepository budgetRepository;
    private ExpenseRepository expenseRepository;
    private HomeSummaryService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        budgetRepository = mock(BudgetRepository.class);
        expenseRepository = mock(ExpenseRepository.class);
        service = new HomeSummaryService(
                userRepository,
                budgetRepository,
                expenseRepository
        );
    }

    @Test
    void returnsAuthenticatedUsersDailyAndWeeklySummary() {
        User user = User.builder().name("절약이").role(User.Role.USER).build();
        Budget budget = mock(Budget.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(budget.getAmount()).thenReturn(35_000);
        when(budgetRepository
                .findFirstByUserIdAndBudgetDateLessThanEqualOrderByBudgetDateDesc(
                        eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(budget));
        when(budgetRepository.sumAmountByUserIdAndPeriod(
                eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(245_000L);
        when(expenseRepository.sumAmountByUserIdAndPeriod(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(12_000L, 61_000L);
        when(expenseRepository.findCategoryIdsBySpendDescending(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(java.util.List.of(1L, 2L));

        var response = service.getSummary(1L);

        assertThat(response.userName()).isEqualTo("절약이");
        assertThat(response.dailyLimit()).isEqualTo(35_000L);
        assertThat(response.spentToday()).isEqualTo(12_000L);
        assertThat(response.remainingToday()).isEqualTo(23_000L);
        assertThat(response.savedToday()).isZero();
        assertThat(response.weeklyBudget()).isEqualTo(245_000L);
        assertThat(response.weeklySpent()).isEqualTo(61_000L);
        assertThat(response.topCategoryId()).isEqualTo(1L);
    }

    @Test
    void returnsZeroBudgetSummaryWhenNewUserHasNoBudgetOrExpense() {
        User user = User.builder().name("신규 사용자").role(User.Role.USER).build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(budgetRepository
                .findFirstByUserIdAndBudgetDateLessThanEqualOrderByBudgetDateDesc(
                        eq(2L), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        var response = service.getSummary(2L);

        assertThat(response.userName()).isEqualTo("신규 사용자");
        assertThat(response.dailyLimit()).isZero();
        assertThat(response.remainingToday()).isZero();
        assertThat(response.spentToday()).isZero();
        assertThat(response.weeklyBudget()).isZero();
        assertThat(response.weeklySpent()).isZero();
    }

    @Test
    void rejectsMissingAuthenticationPrincipal() {
        assertThatThrownBy(() -> service.getSummary(null))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.UNAUTHORIZED));
    }
}
