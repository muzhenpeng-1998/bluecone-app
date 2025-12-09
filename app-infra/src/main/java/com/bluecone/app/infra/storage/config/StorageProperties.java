package com.bluecone.app.infra.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 存储抽象配置。
 */
@ConfigurationProperties(prefix = "bluecone.storage")
public class StorageProperties {

    private Provider provider = Provider.ALIYUN_OSS;

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(final Provider provider) {
        this.provider = provider;
    }

    public enum Provider {
        ALIYUN_OSS,
        MINIO,
        LOCAL
    }
}

