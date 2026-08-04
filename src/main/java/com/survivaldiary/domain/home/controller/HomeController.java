package com.survivaldiary.domain.home.controller;

import com.survivaldiary.domain.home.dto.HomeSummaryResponse;
import com.survivaldiary.domain.home.service.HomeSummaryService;
import com.survivaldiary.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeSummaryService homeSummaryService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<HomeSummaryResponse>> summary(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(homeSummaryService.getSummary(userId)));
    }
}
