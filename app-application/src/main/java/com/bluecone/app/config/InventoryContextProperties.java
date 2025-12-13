package com.bluecone.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 库存上下文中间件配置。
 */
@Component
@ConfigurationProperties(prefix = "bluecone.inventory.context")
public class InventoryContextProperties {

    private boolean enabled = false;
    private List<String> includePaths = List.of("/api/mini/**");
    private List<String> excludePaths = List.of("/ops/**", "/actuator/**", "/api/admin/**");

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
}

