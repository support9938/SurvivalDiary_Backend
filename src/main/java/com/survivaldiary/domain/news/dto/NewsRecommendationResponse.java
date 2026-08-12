package com.survivaldiary.domain.news.dto;

import com.survivaldiary.domain.news.entity.NewsArticle;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "사용자 관심사 기반 맞춤 뉴스")
public record NewsRecommendationResponse(
        Long newsId,
        String category,
        String title,
        String summary,
        String source,
        String sourceUrl,
        LocalDateTime publishedAt,
        String recommendationReason
) {
    public static NewsRecommendationResponse from(
            NewsArticle article,
            String recommendationReason
    ) {
        return new NewsRecommendationResponse(
                article.getId(),
                article.getCategory().getLabel(),
                article.getTitle(),
                article.getSummary(),
                article.getSource(),
                article.getSourceUrl(),
                article.getPublishedAt(),
                recommendationReason
        );
    }
}
