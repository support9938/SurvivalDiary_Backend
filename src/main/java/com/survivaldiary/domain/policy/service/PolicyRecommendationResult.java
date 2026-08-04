package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.dto.PolicyRecommendationStatus;

import java.util.List;

record PolicyRecommendationResult(
        PolicyRecommendationStatus status,
        List<String> reasons,
        int priority
) {
    PolicyRecommendationResult {
        reasons = List.copyOf(reasons);
    }
}
