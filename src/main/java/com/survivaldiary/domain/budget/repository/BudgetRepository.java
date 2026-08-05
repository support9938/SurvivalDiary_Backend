package com.survivaldiary.domain.budget.repository;

import com.survivaldiary.domain.budget.entity.Budget;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByUserIdAndBudgetDate(Long userId, LocalDate budgetDate);

    Optional<Budget> findFirstByUserIdAndBudgetDateLessThanEqualOrderByBudgetDateDesc(
            Long userId,
            LocalDate budgetDate
    );

    @Query("""
            select coalesce(sum(b.amount), 0)
            from Budget b
            where b.userId = :userId
              and b.budgetDate >= :startInclusive
              and b.budgetDate < :endExclusive
            """)
    long sumAmountByUserIdAndPeriod(
            @Param("userId") Long userId,
            @Param("startInclusive") LocalDate startInclusive,
            @Param("endExclusive") LocalDate endExclusive
    );
}
