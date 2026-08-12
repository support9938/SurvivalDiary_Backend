package com.survivaldiary.domain.news.service;

import com.survivaldiary.domain.news.dto.NewsRecommendationResponse;
import com.survivaldiary.domain.news.entity.NewsArticle;
import com.survivaldiary.domain.news.repository.NewsArticleRepository;
import com.survivaldiary.domain.user.entity.User;
import com.survivaldiary.domain.user.repository.UserRepository;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonalizedNewsService {

    private static final Map<String, String> INTEREST_LABELS = Map.of(
            "LIVING_COST", "생활비 절약",
            "HOUSING_COST", "월세·주거비",
            "GOVERNMENT_POLICY", "정부 정책",
            "BENEFIT", "지원금·복지",
            "BUDGETING", "가계부 관리",
            "FOOD_COST", "식비 관리",
            "SAVING_INVESTMENT", "저축·투자",
            "SIDE_INCOME", "부업·소득"
    );

    private final NewsArticleRepository newsArticleRepository;
    private final UserRepository userRepository;
    private final YouthSavingNewsFilter newsFilter;

    @Transactional(readOnly = true)
    public List<NewsRecommendationResponse> recommend(Long userId, int size) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Set<String> userInterests = parseCodes(user.getSignupInterest());
        LocalDateTime now = LocalDateTime.now();

        return newsArticleRepository.findTop1000ByActiveTrueOrderByPublishedAtDesc().stream()
                .filter(article -> newsFilter.isRelevant(
                        article.getTitle(),
                        article.getSummary()
                ))
                .sorted(
                        Comparator.comparingInt(
                                        (NewsArticle article) -> score(article, userInterests, now)
                                )
                                .reversed()
                                .thenComparing(
                                        NewsArticle::getPublishedAt,
                                        Comparator.reverseOrder()
                                )
                )
                .limit(size)
                .map(article -> NewsRecommendationResponse.from(
                        article,
                        recommendationReason(article, userInterests)
                ))
                .toList();
    }

    private int score(
            NewsArticle article,
            Set<String> userInterests,
            LocalDateTime now
    ) {
        Set<String> articleInterests = parseCodes(article.getInterestCodes());
        long matchedInterests = articleInterests.stream()
                .filter(userInterests::contains)
                .count();
        long ageHours = Math.max(
                0,
                Duration.between(article.getPublishedAt(), now).toHours()
        );
        int freshnessScore = ageHours <= 24
                ? 30
                : ageHours <= 24 * 7 ? 20 : ageHours <= 24 * 30 ? 10 : 0;
        return Math.toIntExact(matchedInterests * 100) + freshnessScore;
    }

    private String recommendationReason(
            NewsArticle article,
            Set<String> userInterests
    ) {
        return parseCodes(article.getInterestCodes()).stream()
                .filter(userInterests::contains)
                .findFirst()
                .map(code -> INTEREST_LABELS.getOrDefault(code, code) + " 관심사를 반영했어요.")
                .orElse("최근 생활경제 소식이에요.");
    }

    private Set<String> parseCodes(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        Set<String> values = new LinkedHashSet<>();
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .forEach(values::add);
        return Set.copyOf(values);
    }
}
