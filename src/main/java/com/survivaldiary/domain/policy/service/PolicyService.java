package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.client.YouthPolicyClient;
import com.survivaldiary.domain.policy.client.YouthPolicyResponseParser;
import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;
import com.survivaldiary.domain.policy.client.dto.YouthPolicySearchRequest;
import com.survivaldiary.domain.policy.dto.PolicyCategory;
import com.survivaldiary.domain.policy.dto.PolicyDetail;
import com.survivaldiary.domain.policy.dto.PolicyRecommendationStatus;
import com.survivaldiary.domain.policy.dto.PolicySearchRequest;
import com.survivaldiary.domain.policy.dto.PolicySearchResponse;
import com.survivaldiary.domain.policy.dto.PolicySummary;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class PolicyService {

    private static final int RECOMMENDATION_PROVIDER_PAGE_COUNT = 3;
    private static final int DIVERSE_RECOMMENDATION_COUNT = 6;
    private static final int MAX_RECOMMENDATIONS_PER_CATEGORY = 2;

    private final YouthPolicyClient youthPolicyClient;
    private final YouthPolicyResponseParser responseParser;
    private final PolicyMatcher policyMatcher;
    private final PolicyMapper policyMapper;
    private final PolicyRecommendationEvaluator recommendationEvaluator;

    public PolicyService(
            YouthPolicyClient youthPolicyClient,
            YouthPolicyResponseParser responseParser,
            PolicyMatcher policyMatcher,
            PolicyMapper policyMapper,
            PolicyRecommendationEvaluator recommendationEvaluator
    ) {
        this.youthPolicyClient = youthPolicyClient;
        this.responseParser = responseParser;
        this.policyMatcher = policyMatcher;
        this.policyMapper = policyMapper;
        this.recommendationEvaluator = recommendationEvaluator;
    }

    public PolicySearchResponse search(PolicySearchRequest request) {
        return search(request, 1, false, Set.of());
    }

    public PolicySearchResponse recommend(PolicySearchRequest request) {
        return recommend(request, Set.of());
    }

    public PolicySearchResponse recommend(
            PolicySearchRequest request,
            Set<String> excludedPolicyIds
    ) {
        boolean defaultDiscovery = request.category() == null && request.keyword() == null;
        return search(
                request,
                defaultDiscovery ? RECOMMENDATION_PROVIDER_PAGE_COUNT : 1,
                defaultDiscovery,
                excludedPolicyIds == null ? Set.of() : Set.copyOf(excludedPolicyIds)
        );
    }

    private PolicySearchResponse search(
            PolicySearchRequest request,
            int providerPageCount,
            boolean diversifyRecommendations,
            Set<String> excludedPolicyIds
    ) {
        validateRegionRelation(request);

        Map<String, YouthPolicyItem> candidates = new LinkedHashMap<>();
        int checkedProviderPages = 0;
        int providerPage = request.requestedPage();
        boolean providerMayHaveMore = false;
        Integer retryPage = null;

        for (int index = 0; index < providerPageCount; index++) {
            List<YouthPolicyItem> providerItems;
            try {
                JsonNode root = youthPolicyClient.search(toProviderRequest(request, providerPage));
                providerItems = responseParser.parseItems(root);
            } catch (BusinessException exception) {
                if (checkedProviderPages == 0) {
                    throw exception;
                }
                retryPage = providerPage;
                log.warn(
                        "온통청년 추천 후보 추가 조회 중단: checkedPages={}, retryPage={}, errorCode={}",
                        checkedProviderPages,
                        retryPage,
                        exception.getErrorCode()
                );
                break;
            }

            checkedProviderPages++;
            providerItems.forEach(item -> candidates.putIfAbsent(item.plcyNo(), item));
            providerMayHaveMore = providerItems.size() >= request.requestedSize();
            if (!providerMayHaveMore) {
                break;
            }
            providerPage++;
        }

        Map<String, RankedPolicy> matchedItems = new LinkedHashMap<>();

        for (YouthPolicyItem item : candidates.values()) {
            if (excludedPolicyIds.contains(item.plcyNo())) {
                continue;
            }
            PolicyMatchResult matchResult = policyMatcher.match(item, request);
            if (matchResult.included()) {
                PolicyRecommendationResult recommendationResult = recommendationEvaluator.evaluate(
                        item,
                        request,
                        matchResult
                );
                matchedItems.putIfAbsent(
                        item.plcyNo(),
                        new RankedPolicy(
                                policyMapper.toSummary(item, matchResult, recommendationResult),
                                recommendationResult.priority()
                        )
                );
            }
        }

        List<RankedPolicy> rankedItems = matchedItems.values().stream()
                .sorted(Comparator.comparingInt(RankedPolicy::priority).reversed())
                .toList();
        if (diversifyRecommendations) {
            rankedItems = diversify(rankedItems);
        }

        List<PolicySummary> items = rankedItems.stream()
                .limit(request.requestedSize())
                .map(RankedPolicy::summary)
                .toList();
        Integer nextPage = retryPage != null
                ? retryPage
                : providerMayHaveMore ? providerPage : null;
        return new PolicySearchResponse(
                items,
                nextPage != null,
                checkedProviderPages,
                nextPage
        );
    }

    public PolicyDetail findDetail(String policyId) {
        if (policyId == null || policyId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_POLICY_FILTER);
        }

        JsonNode root = youthPolicyClient.findDetail(policyId.trim());
        YouthPolicyItem item = responseParser.parseDetail(root, policyId.trim());
        return policyMapper.toDetail(item);
    }

    private List<RankedPolicy> diversify(List<RankedPolicy> rankedItems) {
        List<RankedPolicy> recommendations = rankedItems.stream()
                .filter(item -> item.summary().recommendationStatus()
                        == PolicyRecommendationStatus.RECOMMENDED)
                .toList();
        if (recommendations.size() <= MAX_RECOMMENDATIONS_PER_CATEGORY) {
            return rankedItems;
        }

        List<RankedPolicy> diverseTop = new ArrayList<>();
        Map<PolicyCategory, Integer> categoryCounts = new EnumMap<>(PolicyCategory.class);
        Set<String> selectedIds = new HashSet<>();
        for (RankedPolicy item : recommendations) {
            PolicyCategory category = item.summary().categoryType();
            int categoryCount = category == null ? 0 : categoryCounts.getOrDefault(category, 0);
            if (category != null && categoryCount >= MAX_RECOMMENDATIONS_PER_CATEGORY) {
                continue;
            }
            diverseTop.add(item);
            selectedIds.add(item.summary().policyId());
            if (category != null) {
                categoryCounts.put(category, categoryCount + 1);
            }
            if (diverseTop.size() == DIVERSE_RECOMMENDATION_COUNT) {
                break;
            }
        }

        for (RankedPolicy item : recommendations) {
            if (diverseTop.size() == DIVERSE_RECOMMENDATION_COUNT) {
                break;
            }
            if (selectedIds.add(item.summary().policyId())) {
                diverseTop.add(item);
            }
        }

        List<RankedPolicy> diversified = new ArrayList<>(rankedItems.size());
        diversified.addAll(diverseTop);
        for (RankedPolicy item : rankedItems) {
            if (selectedIds.add(item.summary().policyId())) {
                diversified.add(item);
            }
        }
        return List.copyOf(diversified);
    }

    private YouthPolicySearchRequest toProviderRequest(
            PolicySearchRequest request,
            int providerPage
    ) {
        PolicyCategory category = request.requestedCategory();
        String largeCategory = category == null
                ? null
                : switch (category) {
                    case EMPLOYMENT -> "일자리";
                    case HOUSING -> "주거";
                    case EDUCATION -> "교육";
                    case WELFARE_CULTURE -> "복지문화";
                    case PARTICIPATION_RIGHTS -> "참여권리";
                };
        String providerRegionCode = request.districtCode() == null
                ? request.regionCode() + "000"
                : request.districtCode();

        return new YouthPolicySearchRequest(
                providerPage,
                request.requestedSize(),
                providerRegionCode,
                largeCategory,
                null,
                null,
                request.keyword()
        );
    }

    private void validateRegionRelation(PolicySearchRequest request) {
        if (request.districtCode() != null
                && !request.districtCode().startsWith(request.regionCode())) {
            throw new BusinessException(ErrorCode.INVALID_POLICY_FILTER);
        }
    }

    private record RankedPolicy(PolicySummary summary, int priority) {
    }
}
