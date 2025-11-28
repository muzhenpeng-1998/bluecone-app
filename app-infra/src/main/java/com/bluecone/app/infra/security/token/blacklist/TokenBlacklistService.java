package com.bluecone.app.infra.security.token.blacklist;

import java.time.Duration;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.bluecone.app.infra.redis.core.RedisKeyBuilder;
import com.bluecone.app.infra.redis.core.RedisKeyNamespace;
import com.bluecone.app.infra.redis.core.RedisOps;

import lombok.RequiredArgsConstructor;

/**
 * Access Token 黑名单，用于主动登出或踢端。
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisOps redisOps;
    private final RedisKeyBuilder redisKeyBuilder;

    public void blacklistAccessToken(String tokenId, Duration ttl) {
        if (!StringUtils.hasText(tokenId)) {
            return;
        }
        redisOps.setString(buildKey(tokenId), "1", ttl);
    }

    public boolean isAccessTokenBlacklisted(String tokenId) {
        if (!StringUtils.hasText(tokenId)) {
            return false;
        }
        return StringUtils.hasText(redisOps.getString(buildKey(tokenId)));
    }

    private String buildKey(String tokenId) {
        return redisKeyBuilder.build(RedisKeyNamespace.TOKEN_BLACKLIST, tokenId);
    }
}
