package com.survivaldiary.domain.policy.repository;

import com.survivaldiary.domain.policy.entity.HiddenPolicy;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HiddenPolicyRepository extends JpaRepository<HiddenPolicy, Long> {

    Optional<HiddenPolicy> findByUserIdAndPolicyId(Long userId, String policyId);

    Page<HiddenPolicy> findAllByUserId(Long userId, Pageable pageable);

    long deleteByUserIdAndPolicyId(Long userId, String policyId);

    @Query("select hidden.policyId from HiddenPolicy hidden where hidden.userId = :userId")
    Set<String> findPolicyIdsByUserId(@Param("userId") Long userId);
}
