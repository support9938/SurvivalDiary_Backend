package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;

import java.util.regex.Pattern;

final class PolicyInstitutionClassifier {

    private static final Pattern NAMED_UNIVERSITY = Pattern.compile(
            "^[가-힣A-Za-z0-9·&()]+(?:대학교|전문대학|교육대학교|대학)(?:\\s|$).*"
    );

    private PolicyInstitutionClassifier() {
    }

    static boolean isUniversitySpecific(YouthPolicyItem item) {
        if (isNamedUniversity(item.operInstCdNm()) || isNamedUniversity(item.sprvsnInstCdNm())) {
            return true;
        }
        return contains(item.plcyNm(), "대학일자리플러스센터")
                && isNamedUniversity(item.plcyNm());
    }

    private static boolean isNamedUniversity(String value) {
        return value != null && NAMED_UNIVERSITY.matcher(value.trim()).matches();
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }
}
