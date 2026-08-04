package com.survivaldiary.domain.policy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
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

    @Column(name = "work_status", length = 30)
    private String workStatus;

    @Column(name = "job_seeking")
    private Boolean jobSeeking;

    @Column(name = "education_status", length = 30)
    private String educationStatus;

    @Column(name = "employment_status", length = 30)
    private String employmentStatus;

    @Column(name = "income_range", length = 30)
    private String incomeRange;

    @Column(length = 30)
    private String category;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "user_policy_interests",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "interest_code", nullable = false, length = 40)
    private Set<String> interests = new LinkedHashSet<>();

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
            String category,
            String workStatus,
            Boolean jobSeeking,
            String educationStatus,
            Set<String> interests
    ) {
        this.userId = userId;
        update(
                regionCode,
                districtCode,
                employmentStatus,
                incomeRange,
                category,
                workStatus,
                jobSeeking,
                educationStatus,
                interests
        );
    }

    public static PolicyPreference create(
            Long userId,
            String regionCode,
            String districtCode,
            String employmentStatus,
            String incomeRange,
            String category,
            String workStatus,
            Boolean jobSeeking,
            String educationStatus,
            Set<String> interests
    ) {
        return new PolicyPreference(
                userId,
                regionCode,
                districtCode,
                employmentStatus,
                incomeRange,
                category,
                workStatus,
                jobSeeking,
                educationStatus,
                interests
        );
    }

    public void update(
            String regionCode,
            String districtCode,
            String employmentStatus,
            String incomeRange,
            String category,
            String workStatus,
            Boolean jobSeeking,
            String educationStatus,
            Set<String> interests
    ) {
        this.regionCode = regionCode;
        this.districtCode = districtCode;
        this.employmentStatus = employmentStatus;
        this.incomeRange = incomeRange;
        this.category = category;
        this.workStatus = workStatus;
        this.jobSeeking = jobSeeking;
        this.educationStatus = educationStatus;
        this.interests.clear();
        this.interests.addAll(interests == null ? Set.of() : interests);
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
