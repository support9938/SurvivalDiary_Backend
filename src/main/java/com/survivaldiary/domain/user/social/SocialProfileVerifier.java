package com.survivaldiary.domain.user.social;

import com.survivaldiary.domain.user.entity.SocialAccount;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SocialProfileVerifier {

    private final Map<SocialAccount.Provider, SocialProviderClient> clients;

    public SocialProfileVerifier(List<SocialProviderClient> clients) {
        this.clients = new EnumMap<>(SocialAccount.Provider.class);
        clients.forEach(client -> this.clients.put(client.provider(), client));
    }

    public SocialProfile verify(SocialAccount.Provider provider, String accessToken) {
        SocialProviderClient client = clients.get(provider);
        if (client == null) {
            throw new BusinessException(ErrorCode.SOCIAL_AUTH_FAILED);
        }
        return client.verify(accessToken);
    }
}
