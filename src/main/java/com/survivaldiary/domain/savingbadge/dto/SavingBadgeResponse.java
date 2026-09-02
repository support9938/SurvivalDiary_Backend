package com.survivaldiary.domain.savingbadge.dto;

import com.survivaldiary.domain.savingbadge.model.SavingBadgeTier;
import java.time.YearMonth;

public record SavingBadgeResponse(
        String code,
        String label,
        String emoji,
        long thresholdAmount,
        long savedAmount,
        YearMonth earnedMonth,
        String message
) {
    public static SavingBadgeResponse from(
            SavingBadgeTier tier,
            long savedAmount,
            YearMonth earnedMonth
    ) {
        return new SavingBadgeResponse(
                tier.name(),
                tier.getLabel(),
                tier.getEmoji(),
                tier.getThresholdAmount(),
                savedAmount,
                earnedMonth,
                "지난달에는 " + tier.getAchievementText() + "을 절약했어요"
        );
    }
}
