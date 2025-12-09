package com.bluecone.app.resource.infrastructure.config;

import com.bluecone.app.core.id.IdService;
import com.bluecone.app.infra.storage.StorageProperties;
import com.bluecone.app.resource.application.ResourceClientImpl;
import com.bluecone.app.resource.api.ResourceClient;
import com.bluecone.app.resource.domain.service.ResourceDomainService;
import com.bluecone.app.resource.domain.service.ResourcePolicyService;
import com.bluecone.app.resource.infrastructure.repository.ResourceBindingMapper;
import com.bluecone.app.resource.infrastructure.repository.ResourceObjectMapper;
import com.bluecone.app.resource.infrastructure.session.UploadSessionStore;
import com.bluecone.app.resource.infrastructure.storage.StorageClientDelegate;
import com.bluecone.app.resource.support.ResourceProfilesLoader;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 资源中心模块的 Spring 配置，负责暴露对外的 ResourceClient Bean。
 */
@Configuration
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
                                         StorageProperties storageProperties) {
        return new ResourceClientImpl(resourceDomainService,
                resourcePolicyService,
                resourceObjectMapper,
                resourceBindingMapper,
                uploadSessionStore,
                storageClientDelegate,
                profilesLoader,
                idService,
                storageProperties);
    }
}
