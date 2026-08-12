package com.survivaldiary.domain.budget.repository;

import com.survivaldiary.domain.budget.entity.MonthlyBudget;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyBudgetRepository extends JpaRepository<MonthlyBudget, Long> {

    Optional<MonthlyBudget> findByUserIdAndBudgetMonth(Long userId, LocalDate budgetMonth);

    Optional<MonthlyBudget> findFirstByUserIdAndBudgetMonthLessThanEqualOrderByBudgetMonthDesc(
            Long userId,
            LocalDate budgetMonth
    );
}
