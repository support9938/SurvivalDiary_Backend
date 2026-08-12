package com.survivaldiary.domain.user.controller;

import com.survivaldiary.domain.user.dto.LoginRequest;
import com.survivaldiary.domain.user.dto.RefreshTokenRequest;
import com.survivaldiary.domain.user.dto.SignupRequest;
import com.survivaldiary.domain.user.dto.SocialLoginRequest;
import com.survivaldiary.domain.user.dto.TokenResponse;
import com.survivaldiary.domain.user.dto.WebSocialLoginRequest;
import com.survivaldiary.domain.user.dto.WebTokenResponse;
import com.survivaldiary.domain.user.entity.SocialAccount;
import com.survivaldiary.domain.user.service.AuthService;
import com.survivaldiary.global.common.ApiResponse;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

@Tag(name = "Auth", description = "회원가입 / 로그인 / 토큰")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE = "survival_diary_refresh";

    private final AuthService authService;

    @Value("${app.auth.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @Operation(summary = "회원가입",
            description = "이메일 중복 시 409(U001), 검증 실패 시 400(C001)을 반환한다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok());
    }

    @Operation(summary = "로그인",
            description = "이메일/비밀번호 검증 후 액세스(1시간) + 리프레시(2주) 토큰을 발급한다. 실패 시 401(U002).")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @Operation(summary = "웹 이메일 로그인",
            description = "액세스 토큰은 응답으로, 리프레시 토큰은 HttpOnly 쿠키로 발급한다.")
    @PostMapping("/web/login")
    public ResponseEntity<ApiResponse<WebTokenResponse>> webLogin(
            @Valid @RequestBody LoginRequest request) {
        return webLoginResponse(authService.login(request));
    }

    @Operation(summary = "카카오 앱 로그인",
            description = "카카오 액세스 토큰을 공식 사용자 API로 검증한 뒤 서비스 토큰을 발급한다.")
    @PostMapping("/social/kakao")
    public ResponseEntity<ApiResponse<TokenResponse>> loginWithKakao(
            @Valid @RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                authService.socialLogin(SocialAccount.Provider.KAKAO, request)));
    }

    @Operation(summary = "네이버 앱 로그인",
            description = "네이버 액세스 토큰을 공식 사용자 API로 검증한 뒤 서비스 토큰을 발급한다.")
    @PostMapping("/social/naver")
    public ResponseEntity<ApiResponse<TokenResponse>> loginWithNaver(
            @Valid @RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                authService.socialLogin(SocialAccount.Provider.NAVER, request)));
    }

    @PostMapping("/web/social/kakao")
    public ResponseEntity<ApiResponse<WebTokenResponse>> webLoginWithKakao(
            @Valid @RequestBody WebSocialLoginRequest request) {
        return webLoginResponse(
                authService.webSocialLogin(SocialAccount.Provider.KAKAO, request));
    }

    @PostMapping("/web/social/naver")
    public ResponseEntity<ApiResponse<WebTokenResponse>> webLoginWithNaver(
            @Valid @RequestBody WebSocialLoginRequest request) {
        return webLoginResponse(
                authService.webSocialLogin(SocialAccount.Provider.NAVER, request));
    }

    @PostMapping("/web/token/refresh")
    public ResponseEntity<ApiResponse<WebTokenResponse>> refreshWebToken(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return webLoginResponse(authService.refresh(new RefreshTokenRequest(refreshToken)));
    }

    @PostMapping("/web/logout")
    public ResponseEntity<ApiResponse<Void>> logoutWeb(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(new RefreshTokenRequest(refreshToken));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .body(ApiResponse.ok());
    }

    private ResponseEntity<ApiResponse<WebTokenResponse>> webLoginResponse(TokenResponse tokens) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens).toString())
                .body(ApiResponse.ok(WebTokenResponse.from(tokens)));
    }

    private ResponseCookie refreshCookie(TokenResponse tokens) {
        return ResponseCookie.from(REFRESH_COOKIE, tokens.refreshToken())
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Lax")
                .path("/api/auth/web")
                .maxAge(14 * 24 * 60 * 60)
                .build();
    }

    private ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Lax")
                .path("/api/auth/web")
                .maxAge(0)
                .build();
    }

    @Operation(summary = "Refresh access token")
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request)));
    }

    @Operation(summary = "Log out the current device session")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Log out all sessions for the signed-in user")
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @AuthenticationPrincipal Long userId) {
        authService.logoutAll(userId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
