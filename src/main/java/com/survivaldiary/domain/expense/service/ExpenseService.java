package com.survivaldiary.domain.expense.service;

import com.survivaldiary.domain.expense.dto.CreateAutoExpenseRequest;
import com.survivaldiary.domain.expense.dto.CreateExpenseRequest;
import com.survivaldiary.domain.expense.dto.ExpenseResponse;
import com.survivaldiary.domain.expense.entity.Expense;
import com.survivaldiary.domain.expense.repository.ExpenseRepository;
import com.survivaldiary.domain.user.repository.UserRepository;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getMyExpenses(Long authenticatedUserId) {
        if (!userRepository.existsById(authenticatedUserId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return expenseRepository.findAllByUserIdOrderBySpentAtDesc(authenticatedUserId)
                .stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    @Transactional
    public void deleteMyExpense(Long authenticatedUserId, Long expenseId) {
        Expense expense = expenseRepository
                .findByIdAndUserId(expenseId, authenticatedUserId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "삭제할 지출 내역을 찾을 수 없습니다."
                ));
        expenseRepository.delete(expense);
    }

    @Transactional
    public ExpenseResponse createManualExpense(
            Long authenticatedUserId,
            CreateExpenseRequest request) {
        if (!Objects.equals(authenticatedUserId, request.userId())) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "본인의 지출 내역만 저장할 수 있습니다."
            );
        }
        if (!userRepository.existsById(authenticatedUserId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (request.entryType() != Expense.EntryType.MANUAL) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "직접 입력 지출의 등록 방식은 MANUAL이어야 합니다."
            );
        }
        if (request.spentAt().toLocalDate().isAfter(LocalDate.now(BUSINESS_ZONE))) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "지출 날짜는 오늘 이후일 수 없습니다."
            );
        }

        String memo = request.memo() == null || request.memo().isBlank()
                ? null
                : request.memo().trim();
        Expense expense = Expense.manual(
                authenticatedUserId,
                request.categoryId(),
                request.title().trim(),
                normalizeAmount(request.amount()),
                request.spentAt(),
                memo
        );
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse createAutoExpense(
            Long authenticatedUserId,
            CreateAutoExpenseRequest request) {
        validateOwner(authenticatedUserId, request.userId());
        if (!userRepository.existsById(authenticatedUserId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String detectionKey = request.detectionKey().trim();
        return expenseRepository
                .findByUserIdAndDetectionKey(authenticatedUserId, detectionKey)
                .map(ExpenseResponse::from)
                .orElseGet(() -> {
                    String memo = normalizeMemo(request.memo());
                    Expense expense = Expense.auto(
                            authenticatedUserId,
                            request.categoryId(),
                            request.title().trim(),
                            normalizeAmount(request.amount()),
                            request.spentAt(),
                            memo,
                            request.notificationSource().trim(),
                            detectionKey
                    );
                    return ExpenseResponse.from(expenseRepository.save(expense));
                });
    }

    private void validateOwner(Long authenticatedUserId, Long requestedUserId) {
        if (!Objects.equals(authenticatedUserId, requestedUserId)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "본인의 지출 내역만 저장할 수 있습니다."
            );
        }
    }

    private String normalizeMemo(String memo) {
        return memo == null || memo.isBlank() ? null : memo.trim();
    }

    private int normalizeAmount(Long amount) {
        if (amount == null || amount <= 0 || amount > Integer.MAX_VALUE) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "금액은 1원 이상 2,147,483,647원 이하여야 합니다."
            );
        }
        return amount.intValue();
    }
}
