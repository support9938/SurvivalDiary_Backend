package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;
import com.survivaldiary.domain.policy.dto.PolicyApplicationPeriodType;
import com.survivaldiary.domain.policy.dto.PolicyCategory;
import com.survivaldiary.domain.policy.dto.PolicyDetail;
import com.survivaldiary.domain.policy.dto.PolicyOfficialLinkType;
import com.survivaldiary.domain.policy.dto.PolicySummary;
import com.survivaldiary.domain.policy.dto.PolicySupportAmountType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
    private static final String ALWAYS_PERIOD_CODE = "0057002";
    private static final String CLOSED_PERIOD_CODE = "0057003";
    private static final Pattern APPLICATION_PERIOD_PATTERN = Pattern.compile(
            "^\\s*(20\\d{2})[./-]?(\\d{2})[./-]?(\\d{2})"
                    + "\\s*[~∼～]\\s*"
                    + "(20\\d{2})[./-]?(\\d{2})[./-]?(\\d{2})\\s*$"
    );
    private static final Pattern SUPPORT_AMOUNT_PATTERN = Pattern.compile(
            "([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*(억|천만|백만|십만|만|천)?\\s*원"
    );
    private static final Pattern SUPPORT_AMOUNT_RANGE_PATTERN = Pattern.compile(
            "[0-9][0-9,.]*\\s*[~∼～-]\\s*[0-9][0-9,.]*\\s*(?:억|천만|백만|십만|만|천)?\\s*원"
    );
    private static final Pattern NON_GRANT_SUPPORT_PATTERN = Pattern.compile(
            "대출|융자|보증금|이자|수수료|자부담|본인\\s*부담|납부"
    );
    private static final Pattern UNSUPPORTED_AMOUNT_CADENCE_PREFIX_PATTERN = Pattern.compile(
            "(?s).*(?:^|\\s)(?:연간|연|매년|분기|매분기|주|매주|일|매일)\\s*(?:최대\\s*)?$"
    );
    private static final Pattern UNSUPPORTED_AMOUNT_CADENCE_SUFFIX_PATTERN = Pattern.compile(
            "^\\s*(?:/\\s*)?(?:연간|연|년|분기|주|일)(?:\\s|$).*"
    );
    private static final Pattern AMOUNT_QUALIFIER_PATTERN = Pattern.compile(
            "(?:^|\\s|/)(?:매월|월)(?=\\s|최대|$)|최대"
    );
    private static final Pattern LOGIN_LINK_PATTERN = Pattern.compile(
            "(^|[/?&._=-])(login|signin|sign-in|auth|oauth|sso)([/?&._=-]|$)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern HOME_PATH_PATTERN = Pattern.compile(
            "^/(?:index|main|home)?(?:\\.[a-z0-9]+)?/?$",
            Pattern.CASE_INSENSITIVE
    );

    public PolicySummary toSummary(
            YouthPolicyItem item,
            PolicyMatchResult matchResult,
            PolicyRecommendationResult recommendationResult
    ) {
        PolicySupportAmount supportAmount = supportAmount(item.plcySprtCn());
        ApplicationPeriod applicationPeriod = applicationPeriod(item);
        return new PolicySummary(
                item.plcyNo(),
                categoryLabel(item),
                categoryType(item),
                fallback(item.plcyNm(), "정책명을 확인해 주세요."),
                fallback(item.plcyExplnCn(), "정책 상세 내용을 확인해 주세요."),
                supportAmount.amount(),
                supportAmount.type(),
                fallback(item.plcySprtCn(), "지원 내용을 확인해 주세요."),
                blankToNull(item.aplyYmd()),
                applicationPeriod.type(),
                applicationPeriod.startDate(),
                applicationPeriod.endDate(),
                targetText(item),
                fallback(item.sprvsnInstCdNm(), "기관 정보 확인 필요"),
                matchResult.status(),
                matchResult.reasons(),
                recommendationResult.status(),
                recommendationResult.reasons()
        );
    }

    public PolicyDetail toDetail(YouthPolicyItem item) {
        PolicySupportAmount supportAmount = supportAmount(item.plcySprtCn());
        ApplicationPeriod applicationPeriod = applicationPeriod(item);
        String officialUrl = safeUrl(item.aplyUrlAddr());
        return new PolicyDetail(
                item.plcyNo(),
                categoryLabel(item),
                categoryType(item),
                fallback(item.plcyNm(), "정책명을 확인해 주세요."),
                fallback(item.plcyExplnCn(), "정책 상세 내용을 확인해 주세요."),
                supportAmount.amount(),
                supportAmount.type(),
                fallback(item.plcySprtCn(), "지원 내용을 확인해 주세요."),
                blankToNull(item.aplyYmd()),
                applicationPeriod.type(),
                applicationPeriod.startDate(),
                applicationPeriod.endDate(),
                targetText(item),
                fallback(item.sprvsnInstCdNm(), "기관 정보 확인 필요"),
                fallback(item.operInstCdNm(), "운영 기관 정보 확인 필요"),
                fallback(item.plcyAplyMthdCn(), "신청 방법 확인 필요"),
                documents(item.sbmsnDcmntCn()),
                officialUrl,
                officialLinkType(officialUrl),
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

    private PolicySupportAmount supportAmount(String rawSupportText) {
        if (isBlank(rawSupportText)
                || SUPPORT_AMOUNT_RANGE_PATTERN.matcher(rawSupportText).find()
                || NON_GRANT_SUPPORT_PATTERN.matcher(rawSupportText).find()) {
            return PolicySupportAmount.unknown();
        }

        Matcher matcher = SUPPORT_AMOUNT_PATTERN.matcher(rawSupportText);
        if (!matcher.find()) {
            return PolicySupportAmount.unknown();
        }
        String number = matcher.group(1);
        String unit = matcher.group(2);
        int matchStart = matcher.start();
        int matchEnd = matcher.end();
        if (matcher.find()) {
            return PolicySupportAmount.unknown();
        }

        try {
            String prefix = rawSupportText.substring(Math.max(0, matchStart - 16), matchStart);
            String suffix = rawSupportText.substring(
                    matchEnd,
                    Math.min(rawSupportText.length(), matchEnd + 12)
            );
            if (UNSUPPORTED_AMOUNT_CADENCE_PREFIX_PATTERN.matcher(prefix).matches()
                    || UNSUPPORTED_AMOUNT_CADENCE_SUFFIX_PATTERN.matcher(suffix).matches()) {
                return PolicySupportAmount.unknown();
            }

            long amount = new BigDecimal(number.replace(",", ""))
                    .multiply(amountMultiplier(unit))
                    .longValueExact();
            if (amount <= 0) {
                return PolicySupportAmount.unknown();
            }

            boolean monthly = prefix.matches("(?s).*(?:매월|월)\\s*(?:최대\\s*)?$")
                    || prefix.matches("(?s).*최대\\s*(?:매월|월)\\s*$")
                    || suffix.matches("(?s)^\\s*(?:/\\s*)?(?:매월|월)(?:\\s|$).*");
            boolean maximum = prefix.matches("(?s).*최대\\s*(?:(?:매월|월)\\s*)?$")
                    || prefix.matches("(?s).*(?:매월|월)\\s*최대\\s*$");
            boolean hasUnclassifiedQualifier = (AMOUNT_QUALIFIER_PATTERN.matcher(prefix).find()
                    || AMOUNT_QUALIFIER_PATTERN.matcher(suffix).find())
                    && !monthly
                    && !maximum;
            if (hasUnclassifiedQualifier) {
                return PolicySupportAmount.unknown();
            }

            PolicySupportAmountType type;
            if (monthly && maximum) {
                type = PolicySupportAmountType.MONTHLY_MAXIMUM;
            } else if (monthly) {
                type = PolicySupportAmountType.MONTHLY;
            } else if (maximum) {
                type = PolicySupportAmountType.MAXIMUM;
            } else {
                type = PolicySupportAmountType.FIXED;
            }
            return new PolicySupportAmount(amount, type);
        } catch (ArithmeticException exception) {
            return PolicySupportAmount.unknown();
        }
    }

    private BigDecimal amountMultiplier(String unit) {
        return switch (unit == null ? "" : unit) {
            case "억" -> BigDecimal.valueOf(100_000_000L);
            case "천만" -> BigDecimal.valueOf(10_000_000L);
            case "백만" -> BigDecimal.valueOf(1_000_000L);
            case "십만" -> BigDecimal.valueOf(100_000L);
            case "만" -> BigDecimal.valueOf(10_000L);
            case "천" -> BigDecimal.valueOf(1_000L);
            default -> BigDecimal.ONE;
        };
    }

    private ApplicationPeriod applicationPeriod(YouthPolicyItem item) {
        String periodCode = normalize(item.aplyPrdSeCd());
        String periodText = blankToNull(item.aplyYmd());
        String normalizedText = periodText == null ? "" : periodText.replaceAll("\\s+", "");

        if (normalizedText.contains("예산소진")) {
            return new ApplicationPeriod(
                    PolicyApplicationPeriodType.UNTIL_BUDGET,
                    null,
                    null
            );
        }
        if (ALWAYS_PERIOD_CODE.equals(periodCode) || "상시".equals(normalizedText)) {
            return new ApplicationPeriod(PolicyApplicationPeriodType.ALWAYS, null, null);
        }
        if (CLOSED_PERIOD_CODE.equals(periodCode) || "마감".equals(normalizedText)) {
            return new ApplicationPeriod(PolicyApplicationPeriodType.CLOSED, null, null);
        }
        if (!FIXED_PERIOD_CODE.equals(periodCode) || periodText == null) {
            return ApplicationPeriod.unknown();
        }

        Matcher matcher = APPLICATION_PERIOD_PATTERN.matcher(periodText);
        if (!matcher.matches()) {
            return ApplicationPeriod.unknown();
        }

        try {
            LocalDate startDate = localDate(matcher, 1);
            LocalDate endDate = localDate(matcher, 4);
            return endDate.isBefore(startDate)
                    ? ApplicationPeriod.unknown()
                    : new ApplicationPeriod(
                            PolicyApplicationPeriodType.FIXED,
                            startDate,
                            endDate
                    );
        } catch (DateTimeException exception) {
            return ApplicationPeriod.unknown();
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
        URI uri = safeUri(value);
        return uri == null ? null : uri.toString();
    }

    private PolicyOfficialLinkType officialLinkType(String officialUrl) {
        URI uri = safeUri(officialUrl);
        if (uri == null) {
            return PolicyOfficialLinkType.UNAVAILABLE;
        }

        String searchable = Stream.of(uri.getHost(), uri.getPath(), uri.getQuery())
                .filter(value -> value != null)
                .reduce((left, right) -> left + "/" + right)
                .orElse("");
        if (LOGIN_LINK_PATTERN.matcher(searchable).find()) {
            return PolicyOfficialLinkType.LOGIN_REQUIRED;
        }

        String path = uri.getPath();
        boolean homePath = path == null
                || path.isBlank()
                || HOME_PATH_PATTERN.matcher(path).matches();
        if (homePath && (uri.getQuery() == null || uri.getQuery().isBlank())) {
            return PolicyOfficialLinkType.INSTITUTION_HOME;
        }
        return PolicyOfficialLinkType.APPLICATION_CANDIDATE;
    }

    private URI safeUri(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            URI uri = new URI(value.trim());
            boolean allowedScheme = "https".equalsIgnoreCase(uri.getScheme())
                    || "http".equalsIgnoreCase(uri.getScheme());
            if (allowedScheme
                    && uri.getHost() != null
                    && !uri.getHost().isBlank()
                    && uri.getUserInfo() == null) {
                return uri;
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

    private record PolicySupportAmount(Long amount, PolicySupportAmountType type) {
        private static PolicySupportAmount unknown() {
            return new PolicySupportAmount(null, null);
        }
    }

    private record ApplicationPeriod(
            PolicyApplicationPeriodType type,
            LocalDate startDate,
            LocalDate endDate
    ) {
        private static ApplicationPeriod unknown() {
            return new ApplicationPeriod(PolicyApplicationPeriodType.UNKNOWN, null, null);
        }
    }
}
