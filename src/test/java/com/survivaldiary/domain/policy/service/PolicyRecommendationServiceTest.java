package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.dto.PolicyPreferenceResponse;
import com.survivaldiary.domain.policy.dto.PolicyRecommendationRequest;
import com.survivaldiary.domain.policy.dto.PolicySearchRequest;
import com.survivaldiary.domain.policy.dto.PolicySearchResponse;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PolicyRecommendationServiceTest {

    private PolicyPreferenceService preferenceService;
    private PolicyService policyService;
    private HiddenPolicyService hiddenPolicyService;
    private PolicyRecommendationService service;

    @BeforeEach
    void setUp() {
        preferenceService = mock(PolicyPreferenceService.class);
        policyService = mock(PolicyService.class);
        hiddenPolicyService = mock(HiddenPolicyService.class);
        service = new PolicyRecommendationService(
                preferenceService,
                policyService,
                hiddenPolicyService
        );
    }

    @Test
    void 저장된_조건과_탐색_조건을_합쳐_추천한다() {
        when(preferenceService.get(7L)).thenReturn(preference());
        when(hiddenPolicyService.hiddenPolicyIds(7L)).thenReturn(Set.of("HIDDEN-1"));
        PolicySearchResponse response = new PolicySearchResponse(List.of(), false, 1, null);
        when(policyService.recommend(any(), eq(Set.of("HIDDEN-1")))).thenReturn(response);

        PolicySearchResponse result = service.recommend(
                7L,
                new PolicyRecommendationRequest("HOUSING", " 월세 ", 2, 20)
        );

        assertThat(result).isSameAs(response);
        verify(policyService).recommend(org.mockito.ArgumentMatchers.argThat(
                (PolicySearchRequest request) -> request.age() == 29
                        && "26".equals(request.regionCode())
                        && request.districtCode() == null
                        && "HOUSING".equals(request.category())
                        && "월세".equals(request.keyword())
                        && request.requestedPage() == 2
                        && Boolean.TRUE.equals(request.jobSeeking())
                        && "UNIVERSITY_4_YEAR".equals(request.educationLevel())
                        && "ENROLLED".equals(request.enrollmentStatus())
        ), eq(Set.of("HIDDEN-1")));
    }

    @Test
    void 저장된_조건이_없으면_추천을_거절한다() {
        when(preferenceService.get(7L)).thenReturn(PolicyPreferenceResponse.empty(29));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.recommend(
                        7L,
                        new PolicyRecommendationRequest(null, null, 1, 20)
                )
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POLICY_PREFERENCE_REQUIRED);
    }

    private PolicyPreferenceResponse preference() {
        return new PolicyPreferenceResponse(
                true,
                29,
                "26",
                null,
                "JOB_SEEKING",
                null,
                null,
                "UNEMPLOYED",
                true,
                null,
                Set.of(),
                "UNIVERSITY_4_YEAR",
                "ENROLLED"
        );
    }
}
