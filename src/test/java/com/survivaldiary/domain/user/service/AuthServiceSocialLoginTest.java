package com.survivaldiary.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.survivaldiary.domain.user.dto.SocialLoginRequest;
import com.survivaldiary.domain.user.entity.RefreshToken;
import com.survivaldiary.domain.user.entity.SocialAccount;
import com.survivaldiary.domain.user.entity.User;
import com.survivaldiary.domain.user.repository.RefreshTokenRepository;
import com.survivaldiary.domain.user.repository.SocialAccountRepository;
import com.survivaldiary.domain.user.repository.UserRepository;
import com.survivaldiary.domain.user.social.SocialProfile;
import com.survivaldiary.domain.user.social.SocialProfileVerifier;
import com.survivaldiary.global.security.JwtTokenProvider;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceSocialLoginTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RefreshTokenRepository refreshTokenRepository =
            mock(RefreshTokenRepository.class);
    private final SocialAccountRepository socialAccountRepository =
            mock(SocialAccountRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final RefreshTokenHasher refreshTokenHasher = mock(RefreshTokenHasher.class);
    private final SocialProfileVerifier socialProfileVerifier = mock(SocialProfileVerifier.class);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                socialAccountRepository,
                passwordEncoder,
                jwtTokenProvider,
                refreshTokenHasher,
                socialProfileVerifier
        );
        when(jwtTokenProvider.createAccessToken(any(), any())).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(any())).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenValidityMs()).thenReturn(1_800_000L);
        when(jwtTokenProvider.getRefreshTokenValidityMs()).thenReturn(1_209_600_000L);
        when(refreshTokenHasher.hash("refresh-token")).thenReturn("token-hash");
    }

    @Test
    void createsAnIndependentUserForTheFirstSocialLogin() {
        SocialProfile profile = new SocialProfile(
                "provider-user-id", "same@example.com", "SNS 사용자");
        User savedUser = mock(User.class);
        when(savedUser.getId()).thenReturn(17L);
        when(savedUser.getRole()).thenReturn(User.Role.USER);
        when(socialProfileVerifier.verify(SocialAccount.Provider.KAKAO, "provider-token"))
                .thenReturn(profile);
        when(socialAccountRepository.findByProviderAndProviderUserId(
                SocialAccount.Provider.KAKAO, "provider-user-id"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        var response = authService.socialLogin(
                SocialAccount.Provider.KAKAO,
                new SocialLoginRequest("provider-token")
        );

        assertThat(response.accessToken()).isEqualTo("access-token");
        ArgumentCaptor<SocialAccount> accountCaptor =
                ArgumentCaptor.forClass(SocialAccount.class);
        verify(socialAccountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getUserId()).isEqualTo(17L);
        assertThat(accountCaptor.getValue().getProviderUserId())
                .isEqualTo("provider-user-id");
    }

    @Test
    void reusesTheUserLinkedToAnExistingSocialAccount() {
        SocialAccount account = mock(SocialAccount.class);
        User user = mock(User.class);
        when(account.getUserId()).thenReturn(9L);
        when(user.getId()).thenReturn(9L);
        when(user.getRole()).thenReturn(User.Role.USER);
        when(socialProfileVerifier.verify(SocialAccount.Provider.NAVER, "provider-token"))
                .thenReturn(new SocialProfile("naver-id", null, "네이버 사용자"));
        when(socialAccountRepository.findByProviderAndProviderUserId(
                SocialAccount.Provider.NAVER, "naver-id"))
                .thenReturn(Optional.of(account));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));

        var response = authService.socialLogin(
                SocialAccount.Provider.NAVER,
                new SocialLoginRequest("provider-token")
        );

        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(userRepository, never()).save(any(User.class));
        verify(socialAccountRepository, never()).save(any(SocialAccount.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }
}
