package com.survivaldiary.domain.savingbadge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.survivaldiary.domain.expense.repository.ExpenseRepository;
import com.survivaldiary.domain.savingbadge.dto.SavingBadgeResponse;
import com.survivaldiary.domain.savingbadge.dto.UserMonthlyExpenseTotals;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SavingBadgeServiceTest {

    private SavingBadgeService service;
    private final YearMonth earnedMonth = YearMonth.of(2026, 8);

    @BeforeEach
    void setUp() {
        service = new SavingBadgeService(mock(ExpenseRepository.class));
    }

    @Test
    void 절감액에_맞는_가장_높은_음식_뱃지를_계산한다() {
        SavingBadgeResponse badge = service.calculate(totals(80_000, 74_500), earnedMonth);

        assertThat(badge.code()).isEqualTo("LATTE");
        assertThat(badge.label()).isEqualTo("라테 한 잔");
        assertThat(badge.savedAmount()).isEqualTo(5_500);
        assertThat(badge.message()).isEqualTo("지난달에는 라테 한 잔에 가까운 금액을 절약했어요");
    }

    @Test
    void 오백원보다_적게_줄었으면_뱃지를_주지_않는다() {
        assertThat(service.calculate(totals(10_000, 9_501), earnedMonth)).isNull();
    }

    @Test
    void 두_달_중_한_달이라도_기록이_없으면_뱃지를_주지_않는다() {
        UserMonthlyExpenseTotals totals = new UserMonthlyExpenseTotals(1L, 10_000L, 0L, 1L, 0L);

        assertThat(service.calculate(totals, earnedMonth)).isNull();
    }

    @Test
    void 지출이_늘었으면_뱃지를_주지_않는다() {
        assertThat(service.calculate(totals(10_000, 15_000), earnedMonth)).isNull();
    }

    @Test
    void 만원부터_십만원까지_만원_단위_뱃지를_계산한다() {
        Object[][] cases = {
                {10_500L, "GUKBAP"},
                {20_500L, "CHICKEN"},
                {30_500L, "PIZZA"},
                {40_500L, "WHOLE_CAKE"},
                {50_500L, "BOUQUET"},
                {60_500L, "TRANSIT_PASS"},
                {70_500L, "RUNNING_SHOES"},
                {80_500L, "PERFUME"},
                {90_500L, "THEME_PARK_PASS"},
                {100_500L, "WIRELESS_EARBUDS"}
        };

        for (Object[] badgeCase : cases) {
            long savedAmount = (long) badgeCase[0];
            String expectedCode = (String) badgeCase[1];
            SavingBadgeResponse badge = service.calculate(
                    totals(200_000, 200_000 - savedAmount),
                    earnedMonth
            );
            assertThat(badge.code()).isEqualTo(expectedCode);
        }
    }

    private UserMonthlyExpenseTotals totals(long olderAmount, long recentAmount) {
        return new UserMonthlyExpenseTotals(1L, olderAmount, recentAmount, 1L, 1L);
    }
}
