package com.bluecone.app.infra.publicid.verify;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Public ID 索引校验配置。
 */
@ConfigurationProperties(prefix = "bluecone.publicid.verify")
public class PublicIdVerifyProperties {

    /**
     * 是否启用校验。
     */
    private boolean enabled = true;

    /**
     * 校验失败是否直接阻止启动。
     */
    private boolean failFast = true;

    /**
     * 是否强制要求联合索引。
     */
    private boolean requireCompositeIndex = true;

    /**
     * 索引列顺序要求，默认 (tenant_id, public_id)。
     */
    private List<String> indexColumns = List.of("tenant_id", "public_id");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public boolean isRequireCompositeIndex() {
        return requireCompositeIndex;
    }

    public void setRequireCompositeIndex(boolean requireCompositeIndex) {
        this.requireCompositeIndex = requireCompositeIndex;
    }

    public List<String> getIndexColumns() {
        return indexColumns;
    }

    public void setIndexColumns(List<String> indexColumns) {
        this.indexColumns = indexColumns;
    }
}

