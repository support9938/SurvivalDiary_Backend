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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PolicyService {

    static final int MAX_PROVIDER_PAGES = 3;
    static final int PROVIDER_PAGE_SIZE = 20;

    private final YouthPolicyClient youthPolicyClient;
    private final YouthPolicyResponseParser responseParser;
    private final PolicyMatcher policyMatcher;
    private final PolicyMapper policyMapper;

    public PolicyService(
            YouthPolicyClient youthPolicyClient,
            YouthPolicyResponseParser responseParser,
            PolicyMatcher policyMatcher,
            PolicyMapper policyMapper
    ) {
        this.youthPolicyClient = youthPolicyClient;
        this.responseParser = responseParser;
        this.policyMatcher = policyMatcher;
        this.policyMapper = policyMapper;
    }

    public PolicySearchResponse search(PolicySearchRequest request) {
        validateRegionRelation(request);

        Map<String, PolicySummary> matchedItems = new LinkedHashMap<>();
        int checkedPages = 0;
        boolean providerMayHaveMore = true;

        for (int pageNumber = 1;
             pageNumber <= MAX_PROVIDER_PAGES && providerMayHaveMore;
             pageNumber++) {
            JsonNode root = youthPolicyClient.search(toProviderRequest(request, pageNumber));
            List<YouthPolicyItem> providerItems = responseParser.parseItems(root);
            checkedPages++;

            for (YouthPolicyItem item : providerItems) {
                PolicyMatchResult matchResult = policyMatcher.match(item, request);
                if (matchResult.included()) {
                    matchedItems.putIfAbsent(
                            item.plcyNo(),
                            policyMapper.toSummary(item, matchResult)
                    );
                }
            }

            providerMayHaveMore = providerItems.size() >= PROVIDER_PAGE_SIZE;
            if (matchedItems.size() >= request.requestedSize()) {
                break;
            }
        }

        List<PolicySummary> allMatches = new ArrayList<>(matchedItems.values());
        boolean resultLimitExceeded = allMatches.size() > request.requestedSize();
        List<PolicySummary> items = allMatches.stream()
                .limit(request.requestedSize())
                .toList();

        boolean partialResult = resultLimitExceeded || providerMayHaveMore;
        return new PolicySearchResponse(items, partialResult, checkedPages);
    }

    public PolicyDetail findDetail(String policyId) {
        if (policyId == null || policyId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_POLICY_FILTER);
        }

        JsonNode root = youthPolicyClient.findDetail(policyId.trim());
        YouthPolicyItem item = responseParser.parseDetail(root, policyId.trim());
        return policyMapper.toDetail(item);
    }

    private YouthPolicySearchRequest toProviderRequest(
            PolicySearchRequest request,
            int pageNumber
    ) {
        PolicyCategory category = request.requestedCategory();
        String largeCategory = category == null
                ? null
                : switch (category) {
                    case HOUSING -> "주거";
                    case EMPLOYMENT -> "일자리";
                    case CULTURE -> "복지문화";
                    case ASSET, TRANSPORT -> null;
                };
        String providerRegionCode = request.districtCode() == null
                ? request.regionCode() + "000"
                : request.districtCode();

        return new YouthPolicySearchRequest(
                pageNumber,
                PROVIDER_PAGE_SIZE,
                providerRegionCode,
                largeCategory,
                null,
                null,
                null
        );
    }

    private void validateRegionRelation(PolicySearchRequest request) {
        if (request.districtCode() != null
                && !request.districtCode().startsWith(request.regionCode())) {
            throw new BusinessException(ErrorCode.INVALID_POLICY_FILTER);
        }
    }
}
