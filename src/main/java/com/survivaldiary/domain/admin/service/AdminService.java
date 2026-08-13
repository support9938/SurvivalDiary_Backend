package com.survivaldiary.domain.admin.service;

import com.survivaldiary.domain.admin.dto.AdminUserResponse;
import com.survivaldiary.domain.expense.dto.ExpenseResponse;
import com.survivaldiary.domain.expense.repository.ExpenseRepository;
import com.survivaldiary.domain.user.repository.UserRepository;
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
        return expenseRepository.findAllByUserIdOrderBySpentAtDescCreatedAtDesc(userId).stream()
                .map(ExpenseResponse::from)
                .toList();
    }
}
