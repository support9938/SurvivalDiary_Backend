package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;
import com.survivaldiary.domain.policy.dto.PolicyCategory;
import com.survivaldiary.domain.policy.dto.PolicyDetail;
import com.survivaldiary.domain.policy.dto.PolicySummary;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Component
public class PolicyMapper {

    public PolicySummary toSummary(
            YouthPolicyItem item,
            PolicyMatchResult matchResult
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
                targetText(item),
                fallback(item.sprvsnInstCdNm(), "기관 정보 확인 필요"),
                matchResult.status(),
                matchResult.reasons()
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
        String text = String.join(
                " ",
                nullToEmpty(item.plcyNm()),
                nullToEmpty(item.plcyKywdNm()),
                nullToEmpty(item.mclsfNm())
        ).toLowerCase(Locale.ROOT);

        if ("주거".equals(large)) {
            return PolicyCategory.HOUSING;
        }
        if ("일자리".equals(large)) {
            return PolicyCategory.EMPLOYMENT;
        }
        if (text.contains("문화") || text.contains("예술")) {
            return PolicyCategory.CULTURE;
        }
        if (text.contains("자산") || text.contains("금융")) {
            return PolicyCategory.ASSET;
        }
        if (text.contains("교통") || text.contains("대중교통")) {
            return PolicyCategory.TRANSPORT;
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

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
