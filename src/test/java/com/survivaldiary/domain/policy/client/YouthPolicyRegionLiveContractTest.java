package com.survivaldiary.domain.policy.client;

import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;
import com.survivaldiary.domain.policy.client.dto.YouthPolicySearchRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 온통청년이 시도 전체 후보 코드와 시군구 코드를 어떻게 해석하는지 확인하는 실제 계약 테스트.
 * 일반 테스트에서는 실행하지 않으며 인증키, 전체 요청 URL, 정책 원문은 출력하지 않는다.
 */
@EnabledIfEnvironmentVariable(named = "YOUTH_POLICY_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "RUN_YOUTH_POLICY_LIVE_TEST", matches = "(?i)true")
class YouthPolicyRegionLiveContractTest {

    private static final int MAX_PAGES = 3;
    private static final int PAGE_SIZE = 20;
    private static final String BUSAN_ALL_CANDIDATE_CODE = "26000";
    private static final String BUSANJIN_DISTRICT_CODE = "26230";

    @Test
    void 부산_전체_후보와_부산진구의_실제_조회_범위를_비교한다() {
        YouthPolicyProperties properties = new YouthPolicyProperties();
        properties.setApiKey(System.getenv("YOUTH_POLICY_API_KEY"));

        YouthPolicyClientConfig config = new YouthPolicyClientConfig();
        YouthPolicyClient client = new YouthPolicyClient(
                config.youthPolicyRestClient(properties),
                properties
        );
        YouthPolicyResponseParser parser =
                new YouthPolicyResponseParser(new ObjectMapper());

        List<YouthPolicyItem> noRegionItems = fetchItems(client, parser, null);
        List<YouthPolicyItem> busanAllItems = fetchItems(
                client,
                parser,
                BUSAN_ALL_CANDIDATE_CODE
        );
        List<YouthPolicyItem> busanjinItems = fetchItems(
                client,
                parser,
                BUSANJIN_DISTRICT_CODE
        );

        Set<String> busanAllIds = policyIds(busanAllItems);
        Set<String> busanjinIds = policyIds(busanjinItems);
        long sharedPolicyCount = busanjinIds.stream().filter(busanAllIds::contains).count();
        long missingFromBusanAllCount = busanjinIds.size() - sharedPolicyCount;

        System.out.printf(
                "온통청년 지역 계약 결과: 지역없음=%d, 부산전체후보=%d, 부산진구=%d, "
                        + "공통정책=%d, 부산전체에없는부산진구정책=%d%n",
                noRegionItems.size(),
                busanAllItems.size(),
                busanjinItems.size(),
                sharedPolicyCount,
                missingFromBusanAllCount
        );
        System.out.printf(
                "부산 전체 후보 응답 지역 분포: %s%n",
                summarizeRegionScope(busanAllItems)
        );
        System.out.printf(
                "부산진구 응답 지역 분포: %s%n",
                summarizeRegionScope(busanjinItems)
        );

        assertThat(noRegionItems)
                .as("지역 조건이 없는 기준 조회는 정책을 반환해야 합니다.")
                .isNotEmpty();
        assertThat(busanjinItems)
                .as("부산진구 코드는 실제 정책을 반환해야 합니다.")
                .isNotEmpty();
        assertThat(busanAllItems)
                .as("부산 전체 후보 코드는 실제 정책을 반환해야 합니다.")
                .isNotEmpty();
        assertThat(missingFromBusanAllCount)
                .as("확인한 제공처 페이지에서 부산 전체는 부산진구 결과를 포함해야 합니다.")
                .isZero();
        assertThat(busanjinItems)
                .allMatch(item -> item.plcyNo() != null && !item.plcyNo().isBlank());
    }

    private List<YouthPolicyItem> fetchItems(
            YouthPolicyClient client,
            YouthPolicyResponseParser parser,
            String zipCode
    ) {
        Map<String, YouthPolicyItem> uniqueItems = new LinkedHashMap<>();

        for (int pageNumber = 1; pageNumber <= MAX_PAGES; pageNumber++) {
            var root = client.search(new YouthPolicySearchRequest(
                    pageNumber,
                    PAGE_SIZE,
                    zipCode,
                    null,
                    null,
                    null,
                    null
            ));
            List<YouthPolicyItem> pageItems = parser.parseItems(root);
            pageItems.forEach(item -> uniqueItems.putIfAbsent(item.plcyNo(), item));
            if (pageItems.size() < PAGE_SIZE) {
                break;
            }
        }

        return List.copyOf(uniqueItems.values());
    }

    private Set<String> policyIds(List<YouthPolicyItem> items) {
        return items.stream()
                .map(YouthPolicyItem::plcyNo)
                .collect(Collectors.toSet());
    }

    private Map<String, Long> summarizeRegionScope(List<YouthPolicyItem> items) {
        return items.stream().collect(Collectors.groupingBy(
                item -> classifyRegionScope(item.zipCd()),
                LinkedHashMap::new,
                Collectors.counting()
        ));
    }

    private String classifyRegionScope(String zipCodes) {
        if (zipCodes == null || zipCodes.isBlank()) {
            return "지역확인필요";
        }
        if (zipCodes.contains("전국")) {
            return "전국포함";
        }
        if (zipCodes.contains(BUSANJIN_DISTRICT_CODE)) {
            return "부산진구포함";
        }
        if (zipCodes.contains("26")) {
            return "그밖의부산포함";
        }
        return "부산외지역";
    }
}
