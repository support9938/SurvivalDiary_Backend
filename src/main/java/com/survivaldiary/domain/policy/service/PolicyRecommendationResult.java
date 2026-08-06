package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.dto.PolicyRecommendationStatus;
import com.survivaldiary.domain.policy.dto.PolicyMatchSignal;

import java.util.List;

record PolicyRecommendationResult(
        PolicyRecommendationStatus status,
        List<String> reasons,
        List<PolicyMatchSignal> matchSignals,
        int priority
) {
    PolicyRecommendationResult {
        reasons = List.copyOf(reasons);
        matchSignals = List.copyOf(matchSignals);
    }

    PolicyRecommendationResult(
            PolicyRecommendationStatus status,
            List<String> reasons,
            int priority
    ) {
        this(status, reasons, List.of(), priority);
    }
}
