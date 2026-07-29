package com.survivaldiary.global.security;

import com.survivaldiary.global.common.ApiResponse;
import com.survivaldiary.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
// Spring Boot 4는 Jackson 3(tools.jackson)의 ObjectMapper를 빈으로 제공한다 (Jackson 2 빈 없음)
import tools.jackson.databind.ObjectMapper;

/**
 * 미인증 요청 401 응답 — 필터 단계 에러도 ApiResponse 포맷으로 내려준다.
 * (GlobalExceptionHandler는 디스패처 이후만 처리하므로 필터 에러는 여기서 담당)
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object attribute = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        ErrorCode errorCode =
                attribute instanceof ErrorCode code ? code : ErrorCode.UNAUTHORIZED;
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error(errorCode, errorCode.getMessage()));
    }
}
