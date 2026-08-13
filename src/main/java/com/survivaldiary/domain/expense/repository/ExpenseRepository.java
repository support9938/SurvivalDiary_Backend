package com.survivaldiary.domain.expense.repository;

import com.survivaldiary.domain.expense.entity.Expense;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findAllByUserIdOrderBySpentAtDesc(Long userId);

    List<Expense> findAllByUserIdOrderBySpentAtDescCreatedAtDesc(Long userId);

    Optional<Expense> findByIdAndUserId(Long id, Long userId);

    Optional<Expense> findByUserIdAndDetectionKey(Long userId, String detectionKey);

    @Query("""
            select coalesce(sum(e.amount), 0)
            from Expense e
            where e.userId = :userId
              and e.spentAt >= :startInclusive
              and e.spentAt < :endExclusive
            """)
    long sumAmountByUserIdAndPeriod(
            @Param("userId") Long userId,
            @Param("startInclusive") LocalDateTime startInclusive,
            @Param("endExclusive") LocalDateTime endExclusive
    );

    @Query("""
            select e.categoryId
            from Expense e
            where e.userId = :userId
              and e.spentAt >= :startInclusive
              and e.spentAt < :endExclusive
            group by e.categoryId
            order by sum(e.amount) desc, e.categoryId asc
            """)
    List<Long> findCategoryIdsBySpendDescending(
            @Param("userId") Long userId,
            @Param("startInclusive") LocalDateTime startInclusive,
            @Param("endExclusive") LocalDateTime endExclusive
    );

    @Query("""
            select e.categoryId
            from Expense e
            where e.userId = :userId
              and e.spentAt >= :startInclusive
              and e.spentAt < :endExclusive
            group by e.categoryId
            order by sum(e.amount) desc, e.categoryId asc
            """)
    List<Long> findCategoryIdsByMonthlySpendDescending(
            @Param("userId") Long userId,
            @Param("startInclusive") LocalDateTime startInclusive,
            @Param("endExclusive") LocalDateTime endExclusive
    );
}
