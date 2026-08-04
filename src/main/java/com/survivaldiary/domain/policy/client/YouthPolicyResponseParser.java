package com.survivaldiary.domain.policy.client;

import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 성공 응답의 최상위 래퍼가 확정되지 않은 상태에서 정책 객체만 안전하게 찾는다.
 * 실제 키로 응답 구조를 확인하면 래퍼 DTO로 교체할 수 있도록 클라이언트와 분리한다.
 */
@Component
public class YouthPolicyResponseParser {

    private final ObjectMapper objectMapper;

    public YouthPolicyResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<YouthPolicyItem> parseItems(JsonNode root) {
        rejectProviderError(root);

        List<YouthPolicyItem> found = new ArrayList<>();
        collectPolicyItems(root, found);

        Map<String, YouthPolicyItem> uniqueItems = new LinkedHashMap<>();
        for (YouthPolicyItem item : found) {
            if (item.plcyNo() != null && !item.plcyNo().isBlank()) {
                uniqueItems.putIfAbsent(item.plcyNo(), item);
            }
        }
        return List.copyOf(uniqueItems.values());
    }

    public YouthPolicyItem parseDetail(JsonNode root, String policyId) {
        return parseItems(root).stream()
                .filter(item -> policyId.equals(item.plcyNo()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));
    }

    private void collectPolicyItems(JsonNode node, List<YouthPolicyItem> found) {
        if (node == null || node.isNull()) {
            return;
        }

        JsonNode policyId = node.get("plcyNo");
        if (node.isObject() && policyId != null && !policyId.asText("").isBlank()) {
            found.add(toItem(node));
            return;
        }

        node.forEach(child -> collectPolicyItems(child, found));
    }

    private YouthPolicyItem toItem(JsonNode node) {
        try {
            return objectMapper.treeToValue(node, YouthPolicyItem.class);
        } catch (JacksonException exception) {
            throw new BusinessException(ErrorCode.POLICY_PROVIDER_BAD_RESPONSE);
        }
    }

    private void rejectProviderError(JsonNode root) {
        if (root == null || root.isNull()) {
            throw new BusinessException(ErrorCode.POLICY_PROVIDER_BAD_RESPONSE);
        }

        JsonNode errorCode = root.findValue("errorCode");
        if (errorCode != null && !errorCode.asText("").isBlank()) {
            throw new BusinessException(ErrorCode.POLICY_PROVIDER_BAD_RESPONSE);
        }
    }
}
