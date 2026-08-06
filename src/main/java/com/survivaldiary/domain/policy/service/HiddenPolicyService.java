package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.dto.HiddenPolicyRequest;
import com.survivaldiary.domain.policy.dto.HiddenPolicyResponse;
import com.survivaldiary.domain.policy.entity.HiddenPolicy;
import com.survivaldiary.domain.policy.repository.HiddenPolicyRepository;
import com.survivaldiary.domain.user.repository.UserRepository;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HiddenPolicyService {

    private final HiddenPolicyRepository hiddenPolicyRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<HiddenPolicyResponse> list(Long userId, int page, int size) {
        requireUser(userId);
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "hiddenAt")
        );
        return hiddenPolicyRepository.findAllByUserId(userId, pageable)
                .map(HiddenPolicyResponse::from);
    }

    @Transactional
    public HiddenPolicyResponse hide(
            Long userId,
            String policyId,
            HiddenPolicyRequest request
    ) {
        requireUser(userId);
        String normalizedPolicyId = normalizePolicyId(policyId);
        HiddenPolicy hiddenPolicy = hiddenPolicyRepository
                .findByUserIdAndPolicyId(userId, normalizedPolicyId)
                .map(existing -> {
                    existing.updateSnapshot(
                            request.title().trim(),
                            normalizeOptional(request.category()),
                            normalizeOptional(request.shortSummary())
                    );
                    return existing;
                })
                .orElseGet(() -> HiddenPolicy.create(
                        userId,
                        normalizedPolicyId,
                        request.title().trim(),
                        normalizeOptional(request.category()),
                        normalizeOptional(request.shortSummary())
                ));
        return HiddenPolicyResponse.from(hiddenPolicyRepository.save(hiddenPolicy));
    }

    @Transactional
    public void restore(Long userId, String policyId) {
        requireUser(userId);
        hiddenPolicyRepository.deleteByUserIdAndPolicyId(userId, normalizePolicyId(policyId));
    }

    @Transactional(readOnly = true)
    public Set<String> hiddenPolicyIds(Long userId) {
        return hiddenPolicyRepository.findPolicyIdsByUserId(userId);
    }

    private void requireUser(Long userId) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private String normalizePolicyId(String policyId) {
        if (policyId == null || policyId.isBlank() || policyId.trim().length() > 100) {
            throw new BusinessException(ErrorCode.INVALID_POLICY_FILTER);
        }
        return policyId.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
