package com.survivaldiary.global.security;

import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization: Bearer {액세스 토큰} 을 검증해 SecurityContext에 인증을 싣는다.
 * principal = userId(Long) — 컨트롤러에서는 @AuthenticationPrincipal Long userId 로 꺼낸다.
 *
 * 토큰이 없으면 그냥 통과시키고(익명), 보호 경로 접근 시 EntryPoint가 401을 응답한다.
 * 토큰이 있지만 무효/만료면 에러 코드를 request attribute로 남겨 EntryPoint가 구분 응답한다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTRIBUTE = "jwtAuthErrorCode";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                Claims claims = jwtTokenProvider.parse(token);
                if (!JwtTokenProvider.TYPE_ACCESS
                        .equals(claims.get(JwtTokenProvider.CLAIM_TYPE, String.class))) {
                    // 리프레시 토큰으로 API 호출 금지
                    throw new BusinessException(ErrorCode.INVALID_TOKEN);
                }
                Long userId = Long.valueOf(claims.getSubject());
                String role = claims.get(JwtTokenProvider.CLAIM_ROLE, String.class);
                var authentication = new UsernamePasswordAuthenticationToken(
                        userId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (BusinessException e) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, e.getErrorCode());
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
