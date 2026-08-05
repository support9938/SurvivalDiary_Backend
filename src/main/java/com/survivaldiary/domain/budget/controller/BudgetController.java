package com.survivaldiary.domain.budget.controller;

import com.survivaldiary.domain.budget.dto.BudgetAmountRequest;
import com.survivaldiary.domain.budget.dto.BudgetResponse;
import com.survivaldiary.domain.budget.service.BudgetService;
import com.survivaldiary.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Budget", description = "사용자 일일 예산")
@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @Operation(summary = "오늘 예산 조회")
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<BudgetResponse>> getToday(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(budgetService.getToday(userId)));
    }

    @Operation(summary = "오늘 예산 저장")
    @PutMapping("/today")
    public ResponseEntity<ApiResponse<BudgetResponse>> saveToday(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody BudgetAmountRequest request
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(budgetService.saveToday(userId, request)));
    }
}
