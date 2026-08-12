package com.survivaldiary.domain.budget.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "monthly_budgets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monthly_budget_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "budget_month", nullable = false)
    private LocalDate budgetMonth;

    @Column(nullable = false)
    private Integer amount;

    private MonthlyBudget(Long userId, LocalDate budgetMonth, Integer amount) {
        this.userId = userId;
        this.budgetMonth = budgetMonth;
        this.amount = amount;
    }

    public static MonthlyBudget create(Long userId, LocalDate budgetMonth, Integer amount) {
        return new MonthlyBudget(userId, budgetMonth, amount);
    }

    public void updateAmount(Integer amount) {
        this.amount = amount;
    }
}
