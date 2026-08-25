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
import java.util.Set;
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
        assertThat(response.educationLevel()).isEqualTo("UNIVERSITY_4_YEAR");
        assertThat(response.enrollmentStatus()).isEqualTo("ENROLLED");
        verify(preferenceRepository).save(any(PolicyPreference.class));
    }

    @Test
    void 기존_조건은_null_선택값까지_같은_사용자_행에서_전체_교체한다() {
        PolicyPreference existing = PolicyPreference.create(
                7L,
                25,
                "11",
                "11680",
                "JOB_SEEKING",
                "BELOW_100",
                "HOUSING",
                "UNEMPLOYED",
                true,
                null,
                Set.of("HOUSING")
        );
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(preferenceRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(preferenceRepository.save(existing)).thenReturn(existing);

        var response = service.save(7L, request("26", null));

        assertThat(existing.getRegionCode()).isEqualTo("26");
        assertThat(existing.getAge()).isEqualTo(27);
        assertThat(existing.getDistrictCode()).isNull();
        assertThat(existing.getIncomeRange()).isNull();
        assertThat(existing.getCategory()).isNull();
        assertThat(existing.getWorkStatus()).isEqualTo("UNEMPLOYED");
        assertThat(existing.getJobSeeking()).isTrue();
        assertThat(existing.getInterests()).containsExactly("ASSET_BUILDING");
        assertThat(response.regionCode()).isEqualTo("26");
    }

    @Test
    void 확장_추천_조건과_소득_구간을_함께_저장한다() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(preferenceRepository.findById(7L)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(PolicyPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.save(7L, request("11", null, "BELOW_100"));

        assertThat(response.incomeRange()).isEqualTo("BELOW_100");
        assertThat(response.workStatus()).isEqualTo("UNEMPLOYED");
        assertThat(response.interests()).containsExactly("ASSET_BUILDING");
    }

    @Test
    void 생년월일이_없는_소셜_사용자는_입력한_나이를_추천_조건으로_사용한다() {
        User socialUser = User.builder()
                .email("social@example.com")
                .name("소셜 사용자")
                .role(User.Role.USER)
                .build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(socialUser));
        when(preferenceRepository.findById(7L)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(PolicyPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var saved = service.save(7L, request("11", null));
        when(preferenceRepository.findById(7L)).thenReturn(Optional.of(
                PolicyPreference.create(
                        7L,
                        saved.age(),
                        "11",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Set.of()
                )
        ));

        var loaded = service.get(7L);

        assertThat(saved.age()).isEqualTo(27);
        assertThat(loaded.age()).isEqualTo(27);
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
        return request(regionCode, districtCode, null);
    }

    private PolicyPreferenceRequest request(
            String regionCode,
            String districtCode,
            String incomeRange
    ) {
        return new PolicyPreferenceRequest(
                27,
                regionCode,
                districtCode,
                "JOB_SEEKING",
                incomeRange,
                null,
                "UNEMPLOYED",
                true,
                null,
                Set.of("ASSET_BUILDING"),
                "UNIVERSITY_4_YEAR",
                "ENROLLED"
        );
    }
}
