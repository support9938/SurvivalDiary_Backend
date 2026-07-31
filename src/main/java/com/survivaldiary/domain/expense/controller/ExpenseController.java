package com.survivaldiary.domain.expense.controller;

import com.survivaldiary.domain.expense.dto.CreateExpenseRequest;
import com.survivaldiary.domain.expense.dto.ExpenseResponse;
import com.survivaldiary.domain.expense.service.ExpenseService;
import com.survivaldiary.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Expense", description = "지출 내역")
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @Operation(
            summary = "내 지출 목록 조회",
            description = "로그인 사용자가 저장한 지출을 최신순으로 반환한다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getMyExpenses(
            @AuthenticationPrincipal Long authenticatedUserId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(
                        expenseService.getMyExpenses(authenticatedUserId)
                ));
    }

    @Operation(
            summary = "내 지출 삭제",
            description = "로그인 사용자가 본인의 지출 한 건을 삭제한다."
    )
    @DeleteMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long expenseId) {
        expenseService.deleteMyExpense(authenticatedUserId, expenseId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(
            summary = "직접 입력 지출 저장",
            description = "로그인 사용자의 직접 입력 지출을 저장한다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseResponse>> create(
            @AuthenticationPrincipal Long authenticatedUserId,
            @Valid @RequestBody CreateExpenseRequest request) {
        ExpenseResponse response = expenseService.createManualExpense(
                authenticatedUserId,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }
}
