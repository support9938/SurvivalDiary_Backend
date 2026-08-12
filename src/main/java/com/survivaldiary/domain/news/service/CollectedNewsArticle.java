package com.survivaldiary.domain.news.service;

import com.survivaldiary.domain.news.entity.NewsCategory;
import java.time.LocalDateTime;

public record CollectedNewsArticle(
        String externalId,
        NewsCategory category,
        String title,
        String summary,
        String source,
        String sourceUrl,
        String interestCodes,
        LocalDateTime publishedAt
) {
}
