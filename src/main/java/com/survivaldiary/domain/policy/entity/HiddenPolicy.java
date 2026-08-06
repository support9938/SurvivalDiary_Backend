package com.survivaldiary.domain.policy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_hidden_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HiddenPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hidden_policy_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "policy_id", nullable = false, length = 100)
    private String policyId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 100)
    private String category;

    @Column(name = "short_summary", length = 500)
    private String shortSummary;

    @Column(name = "hidden_at", nullable = false)
    private LocalDateTime hiddenAt;

    private HiddenPolicy(
            Long userId,
            String policyId,
            String title,
            String category,
            String shortSummary
    ) {
        this.userId = userId;
        this.policyId = policyId;
        updateSnapshot(title, category, shortSummary);
    }

    public static HiddenPolicy create(
            Long userId,
            String policyId,
            String title,
            String category,
            String shortSummary
    ) {
        return new HiddenPolicy(userId, policyId, title, category, shortSummary);
    }

    public void updateSnapshot(String title, String category, String shortSummary) {
        this.title = title;
        this.category = category;
        this.shortSummary = shortSummary;
        this.hiddenAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        if (hiddenAt == null) {
            hiddenAt = LocalDateTime.now();
        }
    }
}
