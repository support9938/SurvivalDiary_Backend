package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;
import com.survivaldiary.domain.policy.dto.PolicyCategory;
import com.survivaldiary.domain.policy.dto.PolicyDetail;
import com.survivaldiary.domain.policy.dto.PolicySummary;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
public class PolicyMapper {

    private static final String FIXED_PERIOD_CODE = "0057001";
    private static final Pattern APPLICATION_PERIOD_PATTERN = Pattern.compile(
            "^\\s*(20\\d{2})[./-]?(\\d{2})[./-]?(\\d{2})"
                    + "\\s*[~∼～]\\s*"
                    + "(20\\d{2})[./-]?(\\d{2})[./-]?(\\d{2})\\s*$"
    );

    public PolicySummary toSummary(
            YouthPolicyItem item,
            PolicyMatchResult matchResult,
            PolicyRecommendationResult recommendationResult
    ) {
        return new PolicySummary(
                item.plcyNo(),
                categoryLabel(item),
                categoryType(item),
                fallback(item.plcyNm(), "정책명을 확인해 주세요."),
                fallback(item.plcyExplnCn(), "정책 상세 내용을 확인해 주세요."),
                null,
                fallback(item.plcySprtCn(), "지원 내용을 확인해 주세요."),
                blankToNull(item.aplyYmd()),
                applicationEndDate(item),
                targetText(item),
                fallback(item.sprvsnInstCdNm(), "기관 정보 확인 필요"),
                matchResult.status(),
                matchResult.reasons(),
                recommendationResult.status(),
                recommendationResult.reasons()
        );
    }

    public PolicyDetail toDetail(YouthPolicyItem item) {
        return new PolicyDetail(
                item.plcyNo(),
                categoryLabel(item),
                categoryType(item),
                fallback(item.plcyNm(), "정책명을 확인해 주세요."),
                fallback(item.plcyExplnCn(), "정책 상세 내용을 확인해 주세요."),
                null,
                fallback(item.plcySprtCn(), "지원 내용을 확인해 주세요."),
                blankToNull(item.aplyYmd()),
                applicationEndDate(item),
                targetText(item),
                fallback(item.sprvsnInstCdNm(), "기관 정보 확인 필요"),
                fallback(item.operInstCdNm(), "운영 기관 정보 확인 필요"),
                fallback(item.plcyAplyMthdCn(), "신청 방법 확인 필요"),
                documents(item.sbmsnDcmntCn()),
                safeUrl(item.aplyUrlAddr()),
                referenceUrls(item)
        );
    }

    private String categoryLabel(YouthPolicyItem item) {
        if (!isBlank(item.mclsfNm())) {
            return item.mclsfNm().trim();
        }
        return fallback(item.lclsfNm(), "기타");
    }

    private PolicyCategory categoryType(YouthPolicyItem item) {
        String large = normalize(item.lclsfNm());
        if ("일자리".equals(large)) {
            return PolicyCategory.EMPLOYMENT;
        }
        if ("주거".equals(large)) {
            return PolicyCategory.HOUSING;
        }
        if ("교육".equals(large)) {
            return PolicyCategory.EDUCATION;
        }
        if ("복지문화".equals(large)) {
            return PolicyCategory.WELFARE_CULTURE;
        }
        if ("참여권리".equals(large)) {
            return PolicyCategory.PARTICIPATION_RIGHTS;
        }
        return null;
    }

    private String targetText(YouthPolicyItem item) {
        List<String> targets = new ArrayList<>();
        if ("N".equalsIgnoreCase(normalize(item.sprtTrgtAgeLmtYn()))) {
            targets.add("연령 제한 없음");
        } else if (!isBlank(item.sprtTrgtMinAge()) && !isBlank(item.sprtTrgtMaxAge())) {
            targets.add("만 %s~%s세".formatted(
                    item.sprtTrgtMinAge().trim(),
                    item.sprtTrgtMaxAge().trim()
            ));
        }
        if (!isBlank(item.earnEtcCn())) {
            targets.add(item.earnEtcCn().trim());
        }
        return targets.isEmpty()
                ? "지원 대상 조건 확인 필요"
                : String.join(", ", targets);
    }

    private List<String> documents(String rawDocuments) {
        return isBlank(rawDocuments)
                ? List.of()
                : List.of(rawDocuments.trim());
    }

    private LocalDate applicationEndDate(YouthPolicyItem item) {
        if (!FIXED_PERIOD_CODE.equals(normalize(item.aplyPrdSeCd()))
                || isBlank(item.aplyYmd())) {
            return null;
        }

        Matcher matcher = APPLICATION_PERIOD_PATTERN.matcher(item.aplyYmd());
        if (!matcher.matches()) {
            return null;
        }

        try {
            LocalDate startDate = localDate(matcher, 1);
            LocalDate endDate = localDate(matcher, 4);
            return endDate.isBefore(startDate) ? null : endDate;
        } catch (DateTimeException exception) {
            return null;
        }
    }

    private LocalDate localDate(Matcher matcher, int startGroup) {
        return LocalDate.of(
                Integer.parseInt(matcher.group(startGroup)),
                Integer.parseInt(matcher.group(startGroup + 1)),
                Integer.parseInt(matcher.group(startGroup + 2))
        );
    }

    private List<String> referenceUrls(YouthPolicyItem item) {
        return Stream.of(item.refUrlAddr1(), item.refUrlAddr2())
                .map(this::safeUrl)
                .filter(url -> url != null)
                .distinct()
                .toList();
    }

    private String safeUrl(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            URI uri = new URI(value.trim());
            if ("https".equalsIgnoreCase(uri.getScheme())
                    || "http".equalsIgnoreCase(uri.getScheme())) {
                return uri.toString();
            }
        } catch (URISyntaxException exception) {
            return null;
        }
        return null;
    }

    private String fallback(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
