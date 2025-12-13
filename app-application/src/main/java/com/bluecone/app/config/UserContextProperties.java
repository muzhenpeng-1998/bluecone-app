package com.bluecone.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户上下文中间件配置。
 */
@Component
@ConfigurationProperties(prefix = "bluecone.user.context")
public class UserContextProperties {

    private boolean enabled = false;
    private List<String> includePaths = List.of("/api/mini/**");
    private List<String> excludePaths = List.of("/ops/**", "/actuator/**", "/api/admin/**");
    private List<String> allowAnonymousPaths = List.of("/api/auth/**");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getIncludePaths() {
        return includePaths;
    }

    public void setIncludePaths(List<String> includePaths) {
        this.includePaths = includePaths;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    public List<String> getAllowAnonymousPaths() {
        return allowAnonymousPaths;
    }

    public void setAllowAnonymousPaths(List<String> allowAnonymousPaths) {
        this.allowAnonymousPaths = allowAnonymousPaths;
    }
}

