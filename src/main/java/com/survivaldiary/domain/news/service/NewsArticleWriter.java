package com.survivaldiary.domain.news.service;

import com.survivaldiary.domain.news.entity.NewsArticle;
import com.survivaldiary.domain.news.repository.NewsArticleRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NewsArticleWriter {

    private static final String NAVER_EXTERNAL_ID_PREFIX = "naver-";

    private final NewsArticleRepository newsArticleRepository;
    private final YouthSavingNewsFilter newsFilter;

    @Transactional
    public void synchronize(List<CollectedNewsArticle> collectedArticles) {
        Map<String, NewsArticle> existingByExternalId = newsArticleRepository
                .findAllByExternalIdIn(collectedArticles.stream()
                        .map(CollectedNewsArticle::externalId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(
                        NewsArticle::getExternalId,
                        Function.identity()
                ));

        List<NewsArticle> articlesToSave = new ArrayList<>(collectedArticles.size());
        for (CollectedNewsArticle collected : collectedArticles) {
            NewsArticle article = existingByExternalId.get(collected.externalId());
            if (article == null) {
                article = NewsArticle.create(
                        collected.externalId(),
                        collected.category(),
                        collected.title(),
                        collected.summary(),
                        collected.source(),
                        collected.sourceUrl(),
                        collected.interestCodes(),
                        collected.publishedAt()
                );
            } else {
                article.synchronize(
                        collected.category(),
                        collected.title(),
                        collected.summary(),
                        collected.source(),
                        collected.sourceUrl(),
                        collected.interestCodes(),
                        collected.publishedAt()
                );
            }
            articlesToSave.add(article);
        }
        newsArticleRepository.saveAll(articlesToSave);

        List<NewsArticle> irrelevantArticles = newsArticleRepository
                .findAllByExternalIdStartingWith(NAVER_EXTERNAL_ID_PREFIX)
                .stream()
                .filter(article -> !newsFilter.isRelevant(
                        article.getTitle(),
                        article.getSummary()
                ))
                .toList();
        newsArticleRepository.deleteAllInBatch(irrelevantArticles);

        newsArticleRepository.deleteByExternalIdStartingWithAndPublishedAtBefore(
                NAVER_EXTERNAL_ID_PREFIX,
                LocalDateTime.now().minusDays(14)
        );
    }
}
