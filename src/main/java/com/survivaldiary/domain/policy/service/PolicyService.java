package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.client.YouthPolicyClient;
import com.survivaldiary.domain.policy.client.YouthPolicyResponseParser;
import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;
import com.survivaldiary.domain.policy.client.dto.YouthPolicySearchRequest;
import com.survivaldiary.domain.policy.dto.PolicyCategory;
import com.survivaldiary.domain.policy.dto.PolicyDetail;
import com.survivaldiary.domain.policy.dto.PolicySearchRequest;
import com.survivaldiary.domain.policy.dto.PolicySearchResponse;
import com.survivaldiary.domain.policy.dto.PolicySummary;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

@Service
public class PolicyService {

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
        validateRegionRelation(request);

        Map<String, RankedPolicy> matchedItems = new LinkedHashMap<>();
        JsonNode root = youthPolicyClient.search(toProviderRequest(request));
        List<YouthPolicyItem> providerItems = responseParser.parseItems(root);

        for (YouthPolicyItem item : providerItems) {
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

        List<PolicySummary> items = matchedItems.values().stream()
                .sorted(Comparator.comparingInt(RankedPolicy::priority).reversed())
                .map(RankedPolicy::summary)
                .toList();
        boolean providerMayHaveMore = providerItems.size() >= request.requestedSize();
        Integer nextPage = providerMayHaveMore ? request.requestedPage() + 1 : null;
        return new PolicySearchResponse(items, providerMayHaveMore, 1, nextPage);
    }

    public PolicyDetail findDetail(String policyId) {
        if (policyId == null || policyId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_POLICY_FILTER);
        }

        JsonNode root = youthPolicyClient.findDetail(policyId.trim());
        YouthPolicyItem item = responseParser.parseDetail(root, policyId.trim());
        return policyMapper.toDetail(item);
    }

    private YouthPolicySearchRequest toProviderRequest(PolicySearchRequest request) {
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
                request.requestedPage(),
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
