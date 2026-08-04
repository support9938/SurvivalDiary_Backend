package com.survivaldiary.domain.user.repository;

import com.survivaldiary.domain.user.entity.SocialAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUserId(
            SocialAccount.Provider provider,
            String providerUserId
    );
}
