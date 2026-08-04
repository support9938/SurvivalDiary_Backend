package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.dto.PolicyPreferenceRequest;
import com.survivaldiary.domain.policy.entity.PolicyPreference;
import com.survivaldiary.domain.policy.repository.PolicyPreferenceRepository;
import com.survivaldiary.domain.user.entity.User;
import com.survivaldiary.domain.user.repository.UserRepository;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PolicyPreferenceServiceTest {

    private PolicyPreferenceRepository preferenceRepository;
    private UserRepository userRepository;
    private PolicyPreferenceService service;

    @BeforeEach
    void setUp() {
        preferenceRepository = mock(PolicyPreferenceRepository.class);
        userRepository = mock(UserRepository.class);
        service = new PolicyPreferenceService(preferenceRepository, userRepository);
    }

    @Test
    void 저장된_조건이_없으면_사용자_나이와_saved_false를_반환한다() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(preferenceRepository.findById(7L)).thenReturn(Optional.empty());

        var response = service.get(7L);

        assertThat(response.saved()).isFalse();
        assertThat(response.age()).isEqualTo(26);
        assertThat(response.regionCode()).isNull();
    }

    @Test
    void 최초_기본_조건을_사용자_ID로_저장한다() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(preferenceRepository.findById(7L)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(PolicyPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.save(7L, request("11", "11680"));

        assertThat(response.saved()).isTrue();
        assertThat(response.age()).isEqualTo(26);
        assertThat(response.regionCode()).isEqualTo("11");
        assertThat(response.districtCode()).isEqualTo("11680");
        assertThat(response.incomeRange()).isNull();
        verify(preferenceRepository).save(any(PolicyPreference.class));
    }

    @Test
    void 기존_조건은_같은_사용자_행에서_전체_교체한다() {
        PolicyPreference existing = PolicyPreference.create(
                7L,
                "11",
                "11680",
                "JOB_SEEKING",
                "BELOW_100",
                "HOUSING"
        );
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(preferenceRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(preferenceRepository.save(existing)).thenReturn(existing);

        var response = service.save(7L, request("26", null));

        assertThat(existing.getRegionCode()).isEqualTo("26");
        assertThat(existing.getDistrictCode()).isNull();
        assertThat(existing.getIncomeRange()).isNull();
        assertThat(existing.getCategory()).isNull();
        assertThat(response.regionCode()).isEqualTo("26");
    }

    @Test
    void 시도와_시군구_코드가_다르면_저장하지_않는다() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.save(7L, request("11", "26110"))
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_POLICY_FILTER);
        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void 토큰의_사용자가_없으면_사용자_없음으로_거절한다() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.get(99L)
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    private User user() {
        return User.builder()
                .email("policy@example.com")
                .password("encoded-password")
                .name("정책 사용자")
                .birthDate(LocalDate.of(2000, 1, 1))
                .role(User.Role.USER)
                .build();
    }

    private PolicyPreferenceRequest request(String regionCode, String districtCode) {
        return new PolicyPreferenceRequest(
                regionCode,
                districtCode,
                "JOB_SEEKING",
                null,
                null
        );
    }
}
