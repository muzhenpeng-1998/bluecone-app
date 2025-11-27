package com.bluecone.app.infra.cache.profile;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 绑定 application.yml 中对 Profile 的覆盖配置。
 */
@ConfigurationProperties(prefix = "bluecone.cache")
public class CacheProfileProperties {

    /**
     * Key 为 kebab-case 的 profile 名称，例如 user-profile。
     */
    private Map<String, ProfileOverride> profiles = new HashMap<>();

    public Map<String, ProfileOverride> getProfiles() {
        return profiles;
    }

    public void setProfiles(Map<String, ProfileOverride> profiles) {
        this.profiles = profiles;
    }

    public record ProfileOverride(Long ttlSeconds, Boolean cacheNull, Boolean strongConsistency, Boolean hotKey) {
        Duration ttlOrNull() {
            if (ttlSeconds == null) {
                return null;
            }
            return Duration.ofSeconds(ttlSeconds);
        }
    }
}
