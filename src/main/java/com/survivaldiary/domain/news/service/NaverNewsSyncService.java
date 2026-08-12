package com.survivaldiary.domain.news.service;

import com.survivaldiary.domain.news.client.NaverNewsClient;
import com.survivaldiary.domain.news.client.NaverNewsCredentials;
import com.survivaldiary.domain.news.client.NaverNewsItem;
import com.survivaldiary.domain.news.client.NaverNewsProperties;
import com.survivaldiary.domain.news.entity.NewsCategory;
import com.survivaldiary.domain.news.repository.NewsArticleRepository;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverNewsSyncService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final Duration FAILED_RETRY_DELAY = Duration.ofMinutes(5);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final List<NewsTopic> TOPICS = List.of(
            new NewsTopic("청년 생활비 절약", NewsCategory.LIVING_ECONOMY, Set.of("LIVING_COST")),
            new NewsTopic("청년 월세 주거비 절약", NewsCategory.POLICY, Set.of("HOUSING_COST")),
            new NewsTopic("청년 지원금 생활비", NewsCategory.POLICY, Set.of("BENEFIT")),
            new NewsTopic("청년 가계부 예산 관리", NewsCategory.SAVING, Set.of("BUDGETING")),
            new NewsTopic("청년 식비 절약", NewsCategory.SAVING, Set.of("FOOD_COST")),
            new NewsTopic("청년 저축 적금 재테크", NewsCategory.FINANCE, Set.of("SAVING_INVESTMENT")),
            new NewsTopic("청년 교통비 통신비 할인", NewsCategory.LIVING_ECONOMY, Set.of("LIVING_COST")),
            new NewsTopic("청년 금융 혜택 절세", NewsCategory.FINANCE, Set.of("BENEFIT", "SAVING_INVESTMENT"))
    );

    private final NaverNewsClient naverNewsClient;
    private final NaverNewsCredentials credentials;
    private final NaverNewsProperties properties;
    private final YouthSavingNewsFilter newsFilter;
    private final NewsArticleWriter newsArticleWriter;
    private final NewsArticleRepository newsArticleRepository;
    private final ReentrantLock refreshLock = new ReentrantLock();
    private volatile Instant nextRefreshAt = Instant.EPOCH;
    private volatile ErrorCode lastRefreshError;

    public void refreshIfStale() {
        Instant now = Instant.now();
        if (now.isBefore(nextRefreshAt)) {
            throwLastErrorWhenCacheIsEmpty();
            return;
        }
        if (!refreshLock.tryLock()) {
            if (!newsArticleRepository.existsByActiveTrue()) {
                throw new BusinessException(ErrorCode.NEWS_PROVIDER_UNAVAILABLE);
            }
            return;
        }

        try {
            now = Instant.now();
            if (now.isBefore(nextRefreshAt)) {
                return;
            }
            if (!credentials.isConfigured()) {
                recordFailure(now, ErrorCode.NEWS_PROVIDER_UNAVAILABLE);
                log.info("네이버 뉴스 동기화를 건너뜀: API HUB 인증정보 미설정");
                throwLastErrorWhenCacheIsEmpty();
                return;
            }

            nextRefreshAt = now.plus(FAILED_RETRY_DELAY);
            CollectionResult collectionResult = collectByTopic();
            Map<String, MutableCollectedArticle> collected = collectionResult.articles();
            if (collected.isEmpty()) {
                ErrorCode errorCode = collectionResult.successfulRequests() == 0
                        ? collectionResult.lastError()
                        : ErrorCode.NEWS_PROVIDER_BAD_RESPONSE;
                recordFailure(now, errorCode);
                log.warn("네이버 뉴스 동기화 결과가 비어 있음");
                throwLastErrorWhenCacheIsEmpty();
                return;
            }

            List<CollectedNewsArticle> articles = collected.values().stream()
                    .map(MutableCollectedArticle::toCollectedArticle)
                    .toList();
            newsArticleWriter.synchronize(articles);
            nextRefreshAt = now.plus(properties.getCacheTtl());
            lastRefreshError = null;
            log.info("네이버 뉴스 동기화 완료: articleCount={}", articles.size());
        } finally {
            refreshLock.unlock();
        }
    }

    private CollectionResult collectByTopic() {
        Map<String, MutableCollectedArticle> collected = new LinkedHashMap<>();
        int successfulRequests = 0;
        ErrorCode lastError = ErrorCode.NEWS_PROVIDER_UNAVAILABLE;
        for (NewsTopic topic : TOPICS) {
            for (int pageIndex = 0;
                    pageIndex < properties.normalizedPagesPerTopic();
                    pageIndex++) {
                try {
                    List<NaverNewsItem> items = naverNewsClient.searchPage(
                            topic.query(),
                            pageIndex
                    );
                    successfulRequests++;
                    for (NaverNewsItem item : items) {
                        toCollectedArticle(item, topic).ifPresent(article -> collected
                                .compute(article.sourceUrl(), (url, existing) -> {
                                    if (existing == null) {
                                        return article;
                                    }
                                    existing.addInterestCodes(topic.interestCodes());
                                    return existing;
                                }));
                    }
                    if (items.size() < properties.normalizedDisplay()) {
                        break;
                    }
                } catch (BusinessException exception) {
                    lastError = exception.getErrorCode();
                    log.warn(
                            "네이버 뉴스 페이지 수집 실패: query={}, page={}, errorCode={}",
                            topic.query(),
                            pageIndex + 1,
                            exception.getErrorCode().getCode()
                    );
                    break;
                }
            }
        }
        return new CollectionResult(collected, successfulRequests, lastError);
    }

    private void recordFailure(Instant now, ErrorCode errorCode) {
        nextRefreshAt = now.plus(FAILED_RETRY_DELAY);
        lastRefreshError = errorCode;
    }

    private void throwLastErrorWhenCacheIsEmpty() {
        if (lastRefreshError != null && !newsArticleRepository.existsByActiveTrue()) {
            throw new BusinessException(lastRefreshError);
        }
    }

    private java.util.Optional<MutableCollectedArticle> toCollectedArticle(
            NaverNewsItem item,
            NewsTopic topic
    ) {
        String sourceUrl = preferredUrl(item);
        String title = cleanText(item.title());
        String summary = cleanText(item.description());
        LocalDateTime publishedAt = parsePublishedAt(item.publishedAt());
        if (sourceUrl == null
                || sourceUrl.length() > 1000
                || title.isBlank()
                || publishedAt == null
                || !newsFilter.isRelevant(title, summary)) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(new MutableCollectedArticle(
                externalId(sourceUrl),
                topic.category(),
                truncate(title, 300),
                truncate(summary.isBlank() ? title : summary, 500),
                truncate(sourceName(sourceUrl), 100),
                sourceUrl,
                publishedAt,
                topic.interestCodes()
        ));
    }

    private String preferredUrl(NaverNewsItem item) {
        String original = validHttpUrl(item.originalLink());
        return original != null ? original : validHttpUrl(item.link());
    }

    private String validHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(value.trim());
            if (("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null) {
                return uri.toString();
            }
        } catch (IllegalArgumentException ignored) {
            // 유효하지 않은 외부 링크는 기사 목록에서 제외한다.
        }
        return null;
    }

    private LocalDateTime parsePublishedAt(String value) {
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .withZoneSameInstant(SERVICE_ZONE)
                    .toLocalDateTime();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String cleanText(String value) {
        if (value == null) {
            return "";
        }
        String unescaped = HtmlUtils.htmlUnescape(value);
        String withoutTags = HTML_TAG_PATTERN.matcher(unescaped).replaceAll(" ");
        return WHITESPACE_PATTERN.matcher(withoutTags).replaceAll(" ").trim();
    }

    private String sourceName(String sourceUrl) {
        String host = URI.create(sourceUrl).getHost();
        return host == null ? "언론사" : host.replaceFirst("^www\\.", "");
    }

    private String externalId(String sourceUrl) {
        UUID id = UUID.nameUUIDFromBytes(sourceUrl.getBytes(StandardCharsets.UTF_8));
        return "naver-" + id;
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record NewsTopic(
            String query,
            NewsCategory category,
            Set<String> interestCodes
    ) {
    }

    private record CollectionResult(
            Map<String, MutableCollectedArticle> articles,
            int successfulRequests,
            ErrorCode lastError
    ) {
    }

    private static final class MutableCollectedArticle {

        private final String externalId;
        private final NewsCategory category;
        private final String title;
        private final String summary;
        private final String source;
        private final String sourceUrl;
        private final LocalDateTime publishedAt;
        private final Set<String> interestCodes = new LinkedHashSet<>();

        private MutableCollectedArticle(
                String externalId,
                NewsCategory category,
                String title,
                String summary,
                String source,
                String sourceUrl,
                LocalDateTime publishedAt,
                Set<String> interestCodes
        ) {
            this.externalId = externalId;
            this.category = category;
            this.title = title;
            this.summary = summary;
            this.source = source;
            this.sourceUrl = sourceUrl;
            this.publishedAt = publishedAt;
            this.interestCodes.addAll(interestCodes);
        }

        private String sourceUrl() {
            return sourceUrl;
        }

        private void addInterestCodes(Set<String> codes) {
            interestCodes.addAll(codes);
        }

        private CollectedNewsArticle toCollectedArticle() {
            return new CollectedNewsArticle(
                    externalId,
                    category,
                    title,
                    summary,
                    source,
                    sourceUrl,
                    String.join(",", interestCodes),
                    publishedAt
            );
        }
    }
}
