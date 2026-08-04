package com.survivaldiary.domain.user.service;

import com.survivaldiary.domain.user.dto.LoginRequest;
import com.survivaldiary.domain.user.dto.RefreshTokenRequest;
import com.survivaldiary.domain.user.dto.SignupRequest;
import com.survivaldiary.domain.user.dto.SocialLoginRequest;
import com.survivaldiary.domain.user.dto.TokenResponse;
import com.survivaldiary.domain.user.dto.WebSocialLoginRequest;
import com.survivaldiary.domain.user.entity.RefreshToken;
import com.survivaldiary.domain.user.entity.SocialAccount;
import com.survivaldiary.domain.user.entity.User;
import com.survivaldiary.domain.user.repository.RefreshTokenRepository;
import com.survivaldiary.domain.user.repository.SocialAccountRepository;
import com.survivaldiary.domain.user.repository.UserRepository;
import com.survivaldiary.domain.user.social.SocialProfile;
import com.survivaldiary.domain.user.social.SocialProfileVerifier;
import com.survivaldiary.domain.user.social.SocialOAuthTokenClient;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import com.survivaldiary.global.security.JwtTokenProvider;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenHasher refreshTokenHasher;
    private final SocialProfileVerifier socialProfileVerifier;
    private final SocialOAuthTokenClient socialOAuthTokenClient;

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

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse socialLogin(
            SocialAccount.Provider provider,
            SocialLoginRequest request
    ) {
        SocialProfile profile = socialProfileVerifier.verify(provider, request.accessToken());
        User user = socialAccountRepository
                .findByProviderAndProviderUserId(provider, profile.providerUserId())
                .map(account -> userRepository.findById(account.getUserId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND)))
                .orElseGet(() -> createSocialUser(provider, profile));
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse webSocialLogin(
            SocialAccount.Provider provider,
            WebSocialLoginRequest request
    ) {
        String accessToken = socialOAuthTokenClient.exchange(provider, request);
        return socialLogin(provider, new SocialLoginRequest(accessToken));
    }

    private User createSocialUser(
            SocialAccount.Provider provider,
            SocialProfile profile
    ) {
        User user = userRepository.save(User.builder()
                // 이메일이 같다는 이유로 기존 계정과 자동 병합하지 않는다.
                .email(null)
                .password(null)
                .name(socialDisplayName(provider, profile))
                .role(User.Role.USER)
                .build());
        socialAccountRepository.save(SocialAccount.builder()
                .userId(user.getId())
                .provider(provider)
                .providerUserId(profile.providerUserId())
                .build());
        return user;
    }

    private String socialDisplayName(
            SocialAccount.Provider provider,
            SocialProfile profile
    ) {
        if (profile.name() != null && !profile.name().isBlank()) {
            return profile.name().length() <= 50
                    ? profile.name()
                    : profile.name().substring(0, 50);
        }
        return provider == SocialAccount.Provider.KAKAO ? "카카오 생존러" : "네이버 생존러";
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        Claims claims = refreshClaims(request.refreshToken());
        Long userId = Long.valueOf(claims.getSubject());
        RefreshToken current = refreshTokenRepository
                .findByTokenHash(refreshTokenHasher.hash(request.refreshToken()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        if (!current.getUserId().equals(userId) || current.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        refreshTokenRepository.delete(current);
        return issueTokens(user);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        Claims claims = refreshClaims(request.refreshToken());
        Long userId = Long.valueOf(claims.getSubject());
        refreshTokenRepository.findByTokenHash(refreshTokenHasher.hash(request.refreshToken()))
                .filter(token -> token.getUserId().equals(userId))
                .ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public void logoutAll(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    private Claims refreshClaims(String refreshToken) {
        Claims claims = jwtTokenProvider.parse(refreshToken);
        if (!JwtTokenProvider.TYPE_REFRESH
                .equals(claims.get(JwtTokenProvider.CLAIM_TYPE, String.class))) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return claims;
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(refreshTokenHasher.hash(refreshToken))
                .expiresAt(LocalDateTime.now()
                        .plus(Duration.ofMillis(jwtTokenProvider.getRefreshTokenValidityMs())))
                .build());

        return TokenResponse.of(accessToken, refreshToken,
                jwtTokenProvider.getAccessTokenValidityMs());
    }
}
