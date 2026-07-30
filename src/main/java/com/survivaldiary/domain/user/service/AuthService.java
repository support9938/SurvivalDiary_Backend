package com.survivaldiary.domain.user.service;

import com.survivaldiary.domain.user.dto.LoginRequest;
import com.survivaldiary.domain.user.dto.SignupRequest;
import com.survivaldiary.domain.user.dto.TokenResponse;
import com.survivaldiary.domain.user.entity.RefreshToken;
import com.survivaldiary.domain.user.entity.User;
import com.survivaldiary.domain.user.repository.RefreshTokenRepository;
import com.survivaldiary.domain.user.repository.UserRepository;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import com.survivaldiary.global.security.JwtTokenProvider;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public Long signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .phone(request.phone())
                .birthDate(request.birthDate())
                .gender(request.gender())
                .region(request.region())
                .signupInterest(joinSignupInterests(request.signupInterests()))
                .role(User.Role.USER)
                .build();

        return userRepository.save(user).getId();
    }

    private String joinSignupInterests(List<String> signupInterests) {
        if (signupInterests == null || signupInterests.isEmpty()) {
            return null;
        }
        return String.join(",", signupInterests);
    }

    /**
     * BCrypt 검증 후 액세스+리프레시 토큰 발급.
     * 이메일 존재 여부와 비밀번호 불일치를 구분하지 않고 U002 하나로 응답한다 (계정 존재 노출 방지).
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .token(refreshToken)
                .expiresAt(LocalDateTime.now()
                        .plus(Duration.ofMillis(jwtTokenProvider.getRefreshTokenValidityMs())))
                .build());

        return TokenResponse.of(accessToken, refreshToken,
                jwtTokenProvider.getAccessTokenValidityMs());
    }
}
