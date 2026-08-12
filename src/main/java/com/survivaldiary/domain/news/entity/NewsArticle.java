package com.survivaldiary.domain.news.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "news_articles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_id")
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true, length = 100)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NewsCategory category;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(nullable = false, length = 100)
    private String source;

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;

    @Column(name = "interest_codes", nullable = false, length = 500)
    private String interestCodes;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public static NewsArticle create(
            String externalId,
            NewsCategory category,
            String title,
            String summary,
            String source,
            String sourceUrl,
            String interestCodes,
            LocalDateTime publishedAt
    ) {
        NewsArticle article = new NewsArticle();
        article.externalId = externalId;
        article.category = category;
        article.title = title;
        article.summary = summary;
        article.source = source;
        article.sourceUrl = sourceUrl;
        article.interestCodes = interestCodes;
        article.publishedAt = publishedAt;
        article.active = true;
        return article;
    }

    public void synchronize(
            NewsCategory category,
            String title,
            String summary,
            String source,
            String sourceUrl,
            String interestCodes,
            LocalDateTime publishedAt
    ) {
        this.category = category;
        this.title = title;
        this.summary = summary;
        this.source = source;
        this.sourceUrl = sourceUrl;
        this.interestCodes = mergeInterestCodes(this.interestCodes, interestCodes);
        this.publishedAt = publishedAt;
        this.active = true;
    }

    private static String mergeInterestCodes(String current, String incoming) {
        Set<String> codes = new LinkedHashSet<>();
        addCodes(codes, current);
        addCodes(codes, incoming);
        return String.join(",", codes);
    }

    private static void addCodes(Set<String> target, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .forEach(target::add);
    }
}
