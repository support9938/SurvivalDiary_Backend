package com.survivaldiary.domain.budget.service;

import com.survivaldiary.domain.budget.dto.BudgetAmountRequest;
import com.survivaldiary.domain.budget.dto.BudgetResponse;
import com.survivaldiary.domain.budget.entity.Budget;
import com.survivaldiary.domain.budget.entity.MonthlyBudget;
import com.survivaldiary.domain.budget.repository.BudgetRepository;
import com.survivaldiary.domain.budget.repository.MonthlyBudgetRepository;
import com.survivaldiary.domain.user.repository.UserRepository;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private final BudgetRepository budgetRepository;
    private final MonthlyBudgetRepository monthlyBudgetRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public BudgetResponse getToday(Long userId) {
        requireUser(userId);
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        return budgetRepository
                .findFirstByUserIdAndBudgetDateLessThanEqualOrderByBudgetDateDesc(userId, today)
                .map(budget -> new BudgetResponse(today, budget.getAmount(), true))
                .orElseGet(() -> BudgetResponse.empty(today));
    }

    @Transactional
    public BudgetResponse saveToday(Long userId, BudgetAmountRequest request) {
        requireUser(userId);
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        Budget budget = budgetRepository.findByUserIdAndBudgetDate(userId, today)
                .map(existing -> {
                    existing.updateAmount(request.amount());
                    return existing;
                })
                .orElseGet(() -> Budget.create(userId, today, request.amount()));
        Budget saved = budgetRepository.save(budget);
        return new BudgetResponse(today, saved.getAmount(), true);
    }

    @Transactional(readOnly = true)
    public BudgetResponse getMonth(Long userId) {
        requireUser(userId);
        LocalDate month = monthStart();
        return monthlyBudgetRepository
                .findFirstByUserIdAndBudgetMonthLessThanEqualOrderByBudgetMonthDesc(userId, month)
                .map(budget -> new BudgetResponse(month, budget.getAmount(), true))
                .orElseGet(() -> BudgetResponse.empty(month));
    }

    @Transactional
    public BudgetResponse saveMonth(Long userId, BudgetAmountRequest request) {
        requireUser(userId);
        LocalDate month = monthStart();
        MonthlyBudget budget = monthlyBudgetRepository.findByUserIdAndBudgetMonth(userId, month)
                .map(existing -> {
                    existing.updateAmount(request.amount());
                    return existing;
                })
                .orElseGet(() -> MonthlyBudget.create(userId, month, request.amount()));
        MonthlyBudget saved = monthlyBudgetRepository.save(budget);
        return new BudgetResponse(month, saved.getAmount(), true);
    }

    private LocalDate monthStart() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        return today.withDayOfMonth(1);
    }

    private void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }
}
