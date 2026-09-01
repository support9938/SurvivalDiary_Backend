package com.survivaldiary.domain.admin.service;

import com.survivaldiary.domain.admin.dto.AdminUserResponse;
import com.survivaldiary.domain.admin.dto.AdminUserDetailResponse;
import com.survivaldiary.domain.admin.dto.AdminUpdateUserRequest;
import com.survivaldiary.domain.expense.dto.ExpenseResponse;
import com.survivaldiary.domain.expense.repository.ExpenseRepository;
import com.survivaldiary.domain.user.entity.User;
import com.survivaldiary.domain.user.repository.UserRepository;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> users(String query, int page, int size) {
        return userRepository.searchForAdmin(query == null ? "" : query.trim(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(AdminUserResponse::from);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> userExpenses(Long userId) {
        requireUser(userId);
        return expenseRepository.findAllByUserIdOrderBySpentAtDescCreatedAtDesc(userId).stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse user(Long userId) {
        return AdminUserDetailResponse.from(requireUser(userId));
    }

    @Transactional
    public AdminUserDetailResponse updateUser(Long userId, AdminUpdateUserRequest request) {
        User user = requireUser(userId);
        user.updateByAdmin(
                request.name().trim(),
                trimToNull(request.nickname()),
                trimToNull(request.phone()),
                request.birthDate(),
                request.gender(),
                trimToNull(request.region()),
                trimToNull(request.signupInterest()),
                trimToNull(request.bio())
        );
        return AdminUserDetailResponse.from(user);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
