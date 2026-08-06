package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.dto.HiddenPolicyRequest;
import com.survivaldiary.domain.policy.entity.HiddenPolicy;
import com.survivaldiary.domain.policy.repository.HiddenPolicyRepository;
import com.survivaldiary.domain.user.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HiddenPolicyServiceTest {

    private HiddenPolicyRepository repository;
    private UserRepository userRepository;
    private HiddenPolicyService service;

    @BeforeEach
    void setUp() {
        repository = mock(HiddenPolicyRepository.class);
        userRepository = mock(UserRepository.class);
        service = new HiddenPolicyService(repository, userRepository);
        when(userRepository.existsById(7L)).thenReturn(true);
        when(repository.save(any(HiddenPolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void 정책_표시_정보와_계정_ID를_함께_저장한다() {
        when(repository.findByUserIdAndPolicyId(7L, "POLICY-1"))
                .thenReturn(Optional.empty());

        var response = service.hide(
                7L,
                " POLICY-1 ",
                new HiddenPolicyRequest(" 청년 주거 지원 ", " 주거 ", " 월세를 지원해요 ")
        );

        ArgumentCaptor<HiddenPolicy> captor = ArgumentCaptor.forClass(HiddenPolicy.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(response.policyId()).isEqualTo("POLICY-1");
        assertThat(response.title()).isEqualTo("청년 주거 지원");
        assertThat(response.category()).isEqualTo("주거");
        assertThat(response.shortSummary()).isEqualTo("월세를 지원해요");
    }

    @Test
    void 복구는_이미_삭제된_정책이어도_성공한다() {
        service.restore(7L, "POLICY-1");

        verify(repository).deleteByUserIdAndPolicyId(7L, "POLICY-1");
    }

    @Test
    void 추천에서_제외할_정책_ID를_계정별로_조회한다() {
        when(repository.findPolicyIdsByUserId(7L)).thenReturn(Set.of("POLICY-1"));

        assertThat(service.hiddenPolicyIds(7L)).containsExactly("POLICY-1");
    }
}
