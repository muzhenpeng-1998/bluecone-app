package com.bluecone.app.resource.infrastructure.config;

import com.bluecone.app.core.id.IdService;
import com.bluecone.app.infra.storage.config.StorageProperties;
import com.bluecone.app.resource.application.ResourceClientImpl;
import com.bluecone.app.resource.api.ResourceClient;
import com.bluecone.app.resource.config.TenantResourceQuotaProperties;
import com.bluecone.app.resource.domain.service.ResourceDomainService;
import com.bluecone.app.resource.domain.service.ResourcePolicyService;
import com.bluecone.app.resource.domain.service.TenantResourceQuotaService;
import com.bluecone.app.resource.infrastructure.repository.ResourceBindingMapper;
import com.bluecone.app.resource.infrastructure.repository.ResourceObjectMapper;
import com.bluecone.app.resource.infrastructure.session.UploadSessionStore;
import com.bluecone.app.resource.infrastructure.storage.StorageClientDelegate;
import com.bluecone.app.resource.support.ResourceProfilesLoader;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 资源中心模块的 Spring 配置，负责暴露对外的 ResourceClient Bean。
 */
@Configuration
@EnableConfigurationProperties(TenantResourceQuotaProperties.class)
@MapperScan("com.bluecone.app.resource.infrastructure.repository")
public class ResourceModuleConfiguration {

    @Bean
    public ResourceClient resourceClient(ResourceDomainService resourceDomainService,
                                         ResourcePolicyService resourcePolicyService,
                                         ResourceObjectMapper resourceObjectMapper,
                                         ResourceBindingMapper resourceBindingMapper,
                                         UploadSessionStore uploadSessionStore,
                                         StorageClientDelegate storageClientDelegate,
                                         ResourceProfilesLoader profilesLoader,
                                         IdService idService,
                                         StorageProperties storageProperties,
                                         MeterRegistry meterRegistry,
                                         TenantResourceQuotaService quotaService) {
        return new ResourceClientImpl(resourceDomainService,
                resourcePolicyService,
                resourceObjectMapper,
                resourceBindingMapper,
                uploadSessionStore,
                storageClientDelegate,
                profilesLoader,
                idService,
                storageProperties,
                meterRegistry,
                quotaService);
    }
}
