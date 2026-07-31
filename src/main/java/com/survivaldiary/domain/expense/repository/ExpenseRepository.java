package com.survivaldiary.domain.expense.repository;

import com.survivaldiary.domain.expense.entity.Expense;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findAllByUserIdOrderBySpentAtDesc(Long userId);

    Optional<Expense> findByIdAndUserId(Long id, Long userId);
}
