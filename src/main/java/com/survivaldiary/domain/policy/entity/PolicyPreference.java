package com.survivaldiary.domain.policy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_policy_preferences")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyPreference {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "region_code", nullable = false, length = 2)
    private String regionCode;

    @Column(name = "district_code", length = 5)
    private String districtCode;

    @Column(name = "employment_status", nullable = false, length = 30)
    private String employmentStatus;

    @Column(name = "income_range", length = 30)
    private String incomeRange;

    @Column(length = 30)
    private String category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private PolicyPreference(
            Long userId,
            String regionCode,
            String districtCode,
            String employmentStatus,
            String incomeRange,
            String category
    ) {
        this.userId = userId;
        update(regionCode, districtCode, employmentStatus, incomeRange, category);
    }

    public static PolicyPreference create(
            Long userId,
            String regionCode,
            String districtCode,
            String employmentStatus,
            String incomeRange,
            String category
    ) {
        return new PolicyPreference(
                userId,
                regionCode,
                districtCode,
                employmentStatus,
                incomeRange,
                category
        );
    }

    public void update(
            String regionCode,
            String districtCode,
            String employmentStatus,
            String incomeRange,
            String category
    ) {
        this.regionCode = regionCode;
        this.districtCode = districtCode;
        this.employmentStatus = employmentStatus;
        this.incomeRange = incomeRange;
        this.category = category;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
