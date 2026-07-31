package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.dto.PolicyEligibilityStatus;

import java.util.List;

record PolicyMatchResult(
        boolean included,
        PolicyEligibilityStatus status,
        List<String> reasons
) {

    static PolicyMatchResult excluded() {
        return new PolicyMatchResult(false, PolicyEligibilityStatus.MATCHED, List.of());
    }

    static PolicyMatchResult matched() {
        return new PolicyMatchResult(true, PolicyEligibilityStatus.MATCHED, List.of());
    }

    static PolicyMatchResult checkRequired(List<String> reasons) {
        return new PolicyMatchResult(
                true,
                PolicyEligibilityStatus.CHECK_REQUIRED,
                List.copyOf(reasons)
        );
    }
}
