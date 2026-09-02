package com.survivaldiary.domain.expense.repository;

import com.survivaldiary.domain.expense.entity.Expense;
import com.survivaldiary.domain.savingbadge.dto.UserMonthlyExpenseTotals;
import java.time.LocalDateTime;
import java.util.Collection;
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
            select new com.survivaldiary.domain.savingbadge.dto.UserMonthlyExpenseTotals(
                e.userId,
                coalesce(sum(case when e.spentAt >= :olderStart and e.spentAt < :recentStart then e.amount else 0 end), 0),
                coalesce(sum(case when e.spentAt >= :recentStart and e.spentAt < :recentEnd then e.amount else 0 end), 0),
                sum(case when e.spentAt >= :olderStart and e.spentAt < :recentStart then 1 else 0 end),
                sum(case when e.spentAt >= :recentStart and e.spentAt < :recentEnd then 1 else 0 end)
            )
            from Expense e
            where e.userId in :userIds
              and e.spentAt >= :olderStart
              and e.spentAt < :recentEnd
            group by e.userId
            """)
    List<UserMonthlyExpenseTotals> compareMonthlySpending(
            @Param("userIds") Collection<Long> userIds,
            @Param("olderStart") LocalDateTime olderStart,
            @Param("recentStart") LocalDateTime recentStart,
            @Param("recentEnd") LocalDateTime recentEnd
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
