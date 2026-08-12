package com.survivaldiary.domain.user.controller;

import com.survivaldiary.domain.user.dto.UpdateUserRequest;
import com.survivaldiary.domain.user.dto.UpdateDefaultResidenceRequest;
import com.survivaldiary.domain.user.dto.UserResponse;
import com.survivaldiary.domain.user.service.UserService;
import com.survivaldiary.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @Operation(summary = "내 회원 정보 수정")
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateMe(userId, request)));
    }

    @Operation(summary = "Set the default residence used to open the savings map")
    @PatchMapping("/me/default-residence")
    public ResponseEntity<ApiResponse<UserResponse>> updateDefaultResidence(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateDefaultResidenceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateDefaultResidence(userId, request)));
    }

    @Operation(summary = "프로필 사진 등록 또는 수정")
    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> updateProfileImage(
            @AuthenticationPrincipal Long userId,
            @RequestPart("image") MultipartFile image) {
        return ResponseEntity.ok(
                ApiResponse.ok(userService.updateProfileImage(userId, image))
        );
    }

    @Operation(summary = "프로필 사진 삭제")
    @DeleteMapping("/me/profile-image")
    public ResponseEntity<ApiResponse<UserResponse>> deleteProfileImage(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(
                ApiResponse.ok(userService.deleteProfileImage(userId))
        );
    }
}
