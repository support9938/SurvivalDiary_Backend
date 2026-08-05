package com.survivaldiary.domain.home.controller;

import com.survivaldiary.domain.home.dto.HomeSummaryResponse;
import com.survivaldiary.domain.home.service.HomeSummaryService;
import com.survivaldiary.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Home", description = "홈 화면 요약")
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeSummaryController {

    private final HomeSummaryService homeSummaryService;

    @Operation(summary = "내 홈 화면 예산 및 지출 요약 조회")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<HomeSummaryResponse>> getSummary(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(homeSummaryService.getSummary(userId)));
    }
}
