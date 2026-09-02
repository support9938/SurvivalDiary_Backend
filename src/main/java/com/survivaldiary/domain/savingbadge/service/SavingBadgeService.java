package com.survivaldiary.domain.savingbadge.service;

import com.survivaldiary.domain.expense.repository.ExpenseRepository;
import com.survivaldiary.domain.savingbadge.dto.SavingBadgeResponse;
import com.survivaldiary.domain.savingbadge.dto.UserMonthlyExpenseTotals;
import com.survivaldiary.domain.savingbadge.model.SavingBadgeTier;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SavingBadgeService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public SavingBadgeResponse badgeFor(Long userId) {
        return badgesFor(Set.of(userId)).get(userId);
    }

    @Transactional(readOnly = true)
    public Map<Long, SavingBadgeResponse> badgesFor(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();

        YearMonth recentMonth = YearMonth.now(SERVICE_ZONE).minusMonths(1);
        YearMonth olderMonth = recentMonth.minusMonths(1);
        LocalDateTime olderStart = olderMonth.atDay(1).atStartOfDay();
        LocalDateTime recentStart = recentMonth.atDay(1).atStartOfDay();
        LocalDateTime recentEnd = recentMonth.plusMonths(1).atDay(1).atStartOfDay();

        Map<Long, SavingBadgeResponse> badges = new HashMap<>();
        for (UserMonthlyExpenseTotals totals : expenseRepository.compareMonthlySpending(
                userIds, olderStart, recentStart, recentEnd)) {
            SavingBadgeResponse badge = calculate(totals, recentMonth);
            if (badge != null) badges.put(totals.userId(), badge);
        }
        return Map.copyOf(badges);
    }

    public SavingBadgeResponse calculate(UserMonthlyExpenseTotals totals, YearMonth earnedMonth) {
        if (totals == null || totals.olderMonthCount() == 0 || totals.recentMonthCount() == 0) {
            return null;
        }
        long savedAmount = Math.max(0, totals.olderMonthAmount() - totals.recentMonthAmount());
        return SavingBadgeTier.forSavedAmount(savedAmount)
                .map(tier -> SavingBadgeResponse.from(tier, savedAmount, earnedMonth))
                .orElse(null);
    }
}
