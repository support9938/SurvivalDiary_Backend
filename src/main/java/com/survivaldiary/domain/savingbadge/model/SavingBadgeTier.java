package com.survivaldiary.domain.savingbadge.model;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SavingBadgeTier {
    WIRELESS_EARBUDS("무선 이어폰", "🎧", 100_000, "무선 이어폰 하나에 가까운 금액"),
    THEME_PARK_PASS("놀이공원 이용권", "🎢", 90_000, "놀이공원 이용권 하나에 가까운 금액"),
    PERFUME("향수", "🧴", 80_000, "향수 한 병에 가까운 금액"),
    RUNNING_SHOES("러닝화", "👟", 70_000, "러닝화 한 켤레에 가까운 금액"),
    TRANSIT_PASS("교통 정기권", "🚇", 60_000, "교통 정기권 하나에 가까운 금액"),
    BOUQUET("꽃다발", "💐", 50_000, "꽃다발 하나에 가까운 금액"),
    WHOLE_CAKE("홀케이크", "🎂", 40_000, "홀케이크 하나에 가까운 금액"),
    PIZZA("피자 한 판", "🍕", 30_000, "피자 한 판에 가까운 금액"),
    CHICKEN("치킨 한 마리", "🍗", 20_000, "치킨 한 마리에 가까운 금액"),
    GUKBAP("국밥 한 그릇", "🍲", 10_000, "국밥 한 그릇에 가까운 금액"),
    LATTE("라테 한 잔", "🥛", 5_000, "라테 한 잔에 가까운 금액"),
    GIMBAP("김밥 한 줄", "🍙", 4_000, "김밥 한 줄에 가까운 금액"),
    AMERICANO("아메리카노 한 잔", "☕", 3_000, "아메리카노 한 잔에 가까운 금액"),
    ICE_CREAM("아이스크림", "🍦", 1_000, "아이스크림 하나에 가까운 금액"),
    CANDY("사탕", "🍬", 500, "사탕 하나에 가까운 금액");

    private final String label;
    private final String emoji;
    private final long thresholdAmount;
    private final String achievementText;

    public static Optional<SavingBadgeTier> forSavedAmount(long savedAmount) {
        return Arrays.stream(values())
                .filter(tier -> savedAmount >= tier.thresholdAmount)
                .findFirst();
    }
}
