package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.dto.PolicyPreferenceRequest;
import com.survivaldiary.domain.policy.dto.PolicyPreferenceResponse;
import com.survivaldiary.domain.policy.entity.PolicyPreference;
import com.survivaldiary.domain.policy.repository.PolicyPreferenceRepository;
import com.survivaldiary.domain.user.entity.User;
import com.survivaldiary.domain.user.repository.UserRepository;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.Period;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PolicyPreferenceService {

    private final PolicyPreferenceRepository policyPreferenceRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PolicyPreferenceResponse get(Long userId) {
        User user = findUser(userId);
        Integer age = calculateAge(user.getBirthDate());
        return policyPreferenceRepository.findById(userId)
                .map(preference -> PolicyPreferenceResponse.from(preference, age))
                .orElseGet(() -> PolicyPreferenceResponse.empty(age));
    }

    @Transactional
    public PolicyPreferenceResponse save(
            Long userId,
            PolicyPreferenceRequest request
    ) {
        User user = findUser(userId);
        validateRegionRelation(request.regionCode(), request.districtCode());

        PolicyPreference preference = policyPreferenceRepository.findById(userId)
                .map(existing -> {
                    boolean expandedProfile = request.usesExpandedProfile();
                    existing.update(
                            request.regionCode(),
                            request.districtCode(),
                            expandedProfile
                                    ? legacyEmploymentStatus(request)
                                    : request.employmentStatus(),
                            expandedProfile
                                    ? existing.getIncomeRange()
                                    : request.incomeRange(),
                            expandedProfile ? null : request.category(),
                            request.resolvedWorkStatus(),
                            request.resolvedJobSeeking(),
                            request.resolvedEducationStatus(),
                            request.resolvedInterests()
                    );
                    return existing;
                })
                .orElseGet(() -> PolicyPreference.create(
                        userId,
                        request.regionCode(),
                        request.districtCode(),
                        request.usesExpandedProfile()
                                ? legacyEmploymentStatus(request)
                                : request.employmentStatus(),
                        request.incomeRange(),
                        request.usesExpandedProfile() ? null : request.category(),
                        request.resolvedWorkStatus(),
                        request.resolvedJobSeeking(),
                        request.resolvedEducationStatus(),
                        request.resolvedInterests()
                ));

        PolicyPreference saved = policyPreferenceRepository.save(preference);
        return PolicyPreferenceResponse.from(saved, calculateAge(user.getBirthDate()));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateRegionRelation(String regionCode, String districtCode) {
        if (districtCode != null && !districtCode.startsWith(regionCode)) {
            throw new BusinessException(ErrorCode.INVALID_POLICY_FILTER);
        }
    }

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private String legacyEmploymentStatus(PolicyPreferenceRequest request) {
        String workStatus = request.resolvedWorkStatus();
        if ("EMPLOYED".equals(workStatus)) {
            return "EMPLOYED";
        }
        if ("UNEMPLOYED".equals(workStatus)) {
            return Boolean.TRUE.equals(request.resolvedJobSeeking())
                    ? "JOB_SEEKING"
                    : "UNEMPLOYED";
        }
        if (workStatus == null
                && "STUDENT".equals(request.resolvedEducationStatus())) {
            return "STUDENT";
        }
        return null;
    }
}
