package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.dto.PolicyPreferenceResponse;
import com.survivaldiary.domain.policy.dto.PolicyRecommendationRequest;
import com.survivaldiary.domain.policy.dto.PolicySearchRequest;
import com.survivaldiary.domain.policy.dto.PolicySearchResponse;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PolicyRecommendationService {

    private final PolicyPreferenceService policyPreferenceService;
    private final PolicyService policyService;

    public PolicySearchResponse recommend(
            Long userId,
            PolicyRecommendationRequest request
    ) {
        PolicyPreferenceResponse preference = policyPreferenceService.get(userId);
        if (!preference.saved()
                || preference.age() == null
                || preference.regionCode() == null) {
            throw new BusinessException(ErrorCode.POLICY_PREFERENCE_REQUIRED);
        }

        return policyService.recommend(new PolicySearchRequest(
                preference.age(),
                preference.regionCode(),
                preference.districtCode(),
                preference.employmentStatus(),
                preference.incomeRange(),
                request.category(),
                request.keyword(),
                request.page(),
                request.size(),
                preference.workStatus(),
                preference.jobSeeking(),
                preference.educationStatus(),
                preference.interests()
        ));
    }
}
