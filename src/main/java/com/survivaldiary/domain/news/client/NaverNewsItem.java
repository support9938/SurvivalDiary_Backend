package com.survivaldiary.domain.news.client;

public record NaverNewsItem(
        String title,
        String originalLink,
        String link,
        String description,
        String publishedAt
) {
}
