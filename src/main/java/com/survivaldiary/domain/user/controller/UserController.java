package com.survivaldiary.domain.user.controller;

import com.survivaldiary.domain.user.dto.UserResponse;
import com.survivaldiary.domain.user.service.UserService;
import com.survivaldiary.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 필요 API의 견본 — 현재 로그인 사용자 식별 패턴.
 * JwtAuthenticationFilter가 principal에 userId(Long)를 실어주므로
 * 컨트롤러에서는 @AuthenticationPrincipal Long userId 로 꺼내 쓴다. (전 도메인 공통 패턴)
 */
@Tag(name = "User", description = "사용자 정보")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회",
            description = "액세스 토큰의 사용자 정보를 반환한다. 토큰 없음/무효 시 401(C002/U003/U004).")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getMe(userId)));
    }
}
