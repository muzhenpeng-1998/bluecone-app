package com.bluecone.app.infra.storage.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.bluecone.app.infra.storage.StorageClient;
import com.bluecone.app.infra.storage.aliyun.AliyunOssProperties;
import com.bluecone.app.infra.storage.aliyun.AliyunOssStorageClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 存储自动装配。
 */
@Configuration
@EnableConfigurationProperties({StorageProperties.class, AliyunOssProperties.class})
public class StorageAutoConfiguration {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "bluecone.storage", name = "provider", havingValue = "ALIYUN_OSS")
    @ConditionalOnMissingBean
    public OSS aliyunOssClient(final AliyunOssProperties properties) {
        return new OSSClientBuilder().build(properties.getEndpoint(), properties.getAccessKeyId(), properties.getAccessKeySecret());
    }

    @Bean
    @ConditionalOnProperty(prefix = "bluecone.storage", name = "provider", havingValue = "ALIYUN_OSS")
    @ConditionalOnMissingBean(StorageClient.class)
    public StorageClient aliyunOssStorageClient(final OSS ossClient, final AliyunOssProperties properties) {
        return new AliyunOssStorageClient(ossClient, properties);
    }
}

