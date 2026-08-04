package com.survivaldiary.domain.user.social;

import com.survivaldiary.domain.user.entity.SocialAccount;

public interface SocialProviderClient {

    SocialAccount.Provider provider();

    SocialProfile verify(String accessToken);
}
