package com.bluecone.app.infra.redis.support;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 提供当前环境标识用于 Redis key 前缀。
 * 通过 env 前缀避免 dev/test/prod 等环境共享同一套 key 空间，确保多环境隔离。
 */
@Component
public class RedisEnvProvider {

    private static final String DEFAULT_ENV = "dev";

    private final Environment environment;

    @Value("${spring.profiles.active:#{null}}")
    private String activeProfileProperty;

    public RedisEnvProvider(Environment environment) {
        this.environment = environment;
    }

    /**
     * 返回小写的环境名（dev/test/prod/stage），未配置时默认 dev，保证有安全前缀。
     *
     * @return 用于 Redis key 前缀的环境标识
     */
    public String getEnv() {
        String profile = firstActiveProfile();
        if (!StringUtils.hasText(profile)) {
            profile = DEFAULT_ENV;
        }
        return profile.toLowerCase(Locale.ROOT);
    }

    private String firstActiveProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0 && StringUtils.hasText(activeProfiles[0])) {
            return activeProfiles[0];
        }
        return activeProfileProperty;
    }
}
