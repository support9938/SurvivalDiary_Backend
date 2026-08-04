package com.survivaldiary.domain.budget.repository;

import com.survivaldiary.domain.budget.entity.Budget;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByUserIdAndBudgetDate(Long userId, LocalDate budgetDate);

    List<Budget> findAllByUserIdAndBudgetDateBetween(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
}
