package com.survivaldiary.domain.news.controller;

import com.survivaldiary.domain.news.dto.NewsRecommendationResponse;
import com.survivaldiary.domain.news.service.NaverNewsSyncService;
import com.survivaldiary.domain.news.service.PersonalizedNewsService;
import com.survivaldiary.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "News", description = "사용자 관심사 기반 맞춤 뉴스")
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final PersonalizedNewsService personalizedNewsService;
    private final NaverNewsSyncService naverNewsSyncService;

    @Operation(summary = "내 맞춤 뉴스 조회")
    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<List<NewsRecommendationResponse>>> recommendations(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "4") int size
    ) {
        int normalizedSize = Math.min(Math.max(size, 1), 40);
        naverNewsSyncService.refreshIfStale();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(personalizedNewsService.recommend(
                        userId,
                        normalizedSize
                )));
    }
}
