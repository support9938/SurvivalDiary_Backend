package com.survivaldiary.domain.news.repository;

import com.survivaldiary.domain.news.entity.NewsArticle;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    boolean existsByActiveTrue();

    List<NewsArticle> findTop1000ByActiveTrueOrderByPublishedAtDesc();

    Optional<NewsArticle> findByExternalId(String externalId);

    List<NewsArticle> findAllByExternalIdIn(Collection<String> externalIds);

    List<NewsArticle> findAllByExternalIdStartingWith(String externalIdPrefix);

    long deleteByExternalIdStartingWithAndPublishedAtBefore(
            String externalIdPrefix,
            LocalDateTime publishedAt
    );
}
