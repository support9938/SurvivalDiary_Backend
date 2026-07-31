package com.survivaldiary.domain.policy.client.dto;

import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;

public record YouthPolicySearchRequest(
        int pageNumber,
        int pageSize,
        String zipCode,
        String largeCategoryName,
        String middleCategoryName,
        String policyKeyword,
        String policyName
) {

    public YouthPolicySearchRequest {
        if (pageNumber < 1 || pageSize < 1) {
            throw new BusinessException(ErrorCode.INVALID_POLICY_FILTER);
        }
        zipCode = normalize(zipCode);
        largeCategoryName = normalize(largeCategoryName);
        middleCategoryName = normalize(middleCategoryName);
        policyKeyword = normalize(policyKeyword);
        policyName = normalize(policyName);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
