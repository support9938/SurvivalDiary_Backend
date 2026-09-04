package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 같은 신청 공고의 재등록본은 최신 수정본 하나로 취급한다. 사용자 조건은 저장하지 않는다. */
final class PolicyDuplicateResolver {
    private static final int MAX_ENTRIES = 1000;
    private static final long RETENTION_SECONDS = 600;
    private final Map<String, RememberedPolicy> latestByKey = new LinkedHashMap<>();

    synchronized List<YouthPolicyItem> reconcile(
            Collection<YouthPolicyItem> items,
            Set<String> excludedIds
    ) {
        Instant now = Instant.now();
        latestByKey.entrySet().removeIf(entry ->
                entry.getValue().observedAt().plusSeconds(RETENTION_SECONDS).isBefore(now));
        for (YouthPolicyItem item : items) {
            String key = duplicateKey(item);
            if (key == null) {
                continue;
            }
            RememberedPolicy previous = latestByKey.get(key);
            if (previous == null || isNewer(item, previous.item())) {
                latestByKey.put(key, new RememberedPolicy(item, now));
            }
        }

        Set<String> excludedKeys = items.stream()
                .filter(item -> excludedIds.contains(item.plcyNo()))
                .map(PolicyDuplicateResolver::duplicateKey)
                .filter(key -> key != null)
                .collect(Collectors.toSet());
        Map<String, YouthPolicyItem> result = new LinkedHashMap<>();
        for (YouthPolicyItem item : items) {
            String key = duplicateKey(item);
            YouthPolicyItem latest = key == null ? item : latestByKey.get(key).item();
            if (!excludedIds.contains(item.plcyNo())
                    && !excludedIds.contains(latest.plcyNo())
                    && !excludedKeys.contains(key)) {
                result.putIfAbsent(key == null ? "id:" + item.plcyNo() : key, latest);
            }
        }
        while (latestByKey.size() > MAX_ENTRIES) {
            latestByKey.remove(latestByKey.keySet().iterator().next());
        }
        return List.copyOf(result.values());
    }

    static String duplicateKey(YouthPolicyItem item) {
        if (item.plcyNm() == null || item.plcyNm().isBlank()
                || sourceUpdatedAt(item) == null || item.aplyUrlAddr() == null) {
            return null;
        }
        try {
            URI uri = URI.create(item.aplyUrlAddr().trim());
            if (uri.getHost() == null || uri.getUserInfo() != null
                    || !("https".equalsIgnoreCase(uri.getScheme())
                    || "http".equalsIgnoreCase(uri.getScheme()))) {
                return null;
            }
            String path = uri.getRawPath() == null ? "" : uri.getRawPath();
            String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
            // 홈페이지·공용 로그인 링크만 같은 별개 모집은 합치지 않는다.
            if ((query.isEmpty() && (path.isEmpty() || path.equals("/")
                    || path.matches("(?i).*/(?:index|main|home)(?:\\.[a-z]+)?/?")))
                    || path.toLowerCase(Locale.ROOT).matches(".*(?:login|signin|joinagree).*")) {
                return null;
            }
            String title = item.plcyNm().trim().replaceAll("\\s+", " ");
            String identity = title + "\n" + uri.getScheme().toLowerCase(Locale.ROOT)
                    + "://" + uri.getHost().toLowerCase(Locale.ROOT)
                    + (uri.getPort() < 0 ? "" : ":" + uri.getPort()) + path + query;
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(StandardCharsets.UTF_8)));
        } catch (IllegalArgumentException | NoSuchAlgorithmException exception) {
            return null;
        }
    }

    static LocalDateTime sourceUpdatedAt(YouthPolicyItem item) {
        if (item.lastMdfcnDt() == null) {
            return null;
        }
        String value = item.lastMdfcnDt().trim();
        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(
                        java.time.format.ResolverStyle.STRICT),
                DateTimeFormatter.ofPattern("uuuuMMddHHmmss").withResolverStyle(
                        java.time.format.ResolverStyle.STRICT))) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // 다른 공식 날짜 표현을 순서대로 확인한다.
            }
        }
        return null;
    }

    static boolean isNewer(YouthPolicyItem candidate, YouthPolicyItem previous) {
        LocalDateTime candidateTime = sourceUpdatedAt(candidate);
        LocalDateTime previousTime = sourceUpdatedAt(previous);
        return candidateTime != null && (previousTime == null || candidateTime.isAfter(previousTime));
    }

    private record RememberedPolicy(YouthPolicyItem item, Instant observedAt) { }
}
