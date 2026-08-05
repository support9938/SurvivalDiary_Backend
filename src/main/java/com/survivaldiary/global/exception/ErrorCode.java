package com.survivaldiary.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 전역 에러 코드.
 *
 * 코드 체계: {도메인 접두사}{3자리 번호}
 *   C: 공통 / U: 사용자·인증 / P: 게시글 / E: 지출·예산 / Y: 정책 / L: 장소
 * 도메인 작업 시 담당자가 본인 접두사 아래 코드를 추가한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C002", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C003", "접근 권한이 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C005", "서버 오류가 발생했습니다."),

    // 사용자 / 인증
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U001", "이미 사용 중인 이메일입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "U002", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "U003", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "U004", "만료된 토큰입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U005", "사용자를 찾을 수 없습니다."),
    SOCIAL_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "U006", "SNS 인증 정보를 확인할 수 없습니다."),

    // 정책
    POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "Y001", "정책을 찾을 수 없습니다."),
    POLICY_PROVIDER_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Y002",
            "정책 정보를 불러올 수 없습니다. 잠시 후 다시 시도해 주세요."
    ),
    POLICY_PROVIDER_BAD_RESPONSE(
            HttpStatus.BAD_GATEWAY,
            "Y003",
            "정책 제공처 응답을 처리할 수 없습니다."
    ),
    INVALID_POLICY_FILTER(HttpStatus.BAD_REQUEST, "Y004", "정책 검색 조건이 올바르지 않습니다."),
    POLICY_PREFERENCE_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "Y005",
            "맞춤 정책 추천을 위한 기본 조건을 먼저 저장해 주세요."
    ),

    // 장소 / 지도
    MAP_PROVIDER_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "L001",
            "지도 정보를 불러올 수 없습니다. 잠시 후 다시 시도해 주세요."
    ),
    MAP_PROVIDER_BAD_RESPONSE(
            HttpStatus.BAD_GATEWAY,
            "L002",
            "지도 정보 제공처 응답을 처리할 수 없습니다."
    ),
    INVALID_MAP_FILTER(HttpStatus.BAD_REQUEST, "L003", "지도 검색 조건이 올바르지 않습니다."),
    MAP_ROUTE_NOT_FOUND(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "L004",
            "출발지에서 목적지까지 이동 가능한 도보 경로를 찾을 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
