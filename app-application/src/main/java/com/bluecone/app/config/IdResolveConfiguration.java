package com.bluecone.app.config;

import java.time.Clock;
import java.util.List;

import com.bluecone.app.core.idresolve.api.PublicIdRegistrar;
import com.bluecone.app.core.idresolve.api.PublicIdResolver;
import com.bluecone.app.core.idresolve.application.CachedPublicIdResolver;
import com.bluecone.app.core.idresolve.application.DefaultPublicIdRegistrar;
import com.bluecone.app.core.idresolve.config.IdResolveProperties;
import com.bluecone.app.core.idresolve.spi.PublicIdFallbackLookup;
import com.bluecone.app.core.idresolve.spi.PublicIdL2Cache;
import com.bluecone.app.core.idresolve.spi.PublicIdMapRepository;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 公共 ID 解析能力装配配置。
 */
@Configuration
@ConditionalOnClass(PublicIdCodec.class)
@ConditionalOnProperty(prefix = "bluecone.idresolve", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IdResolveConfiguration {

    @Bean
    public PublicIdResolver publicIdResolver(PublicIdMapRepository repository,
                                             ObjectProvider<PublicIdL2Cache> l2CacheProvider,
                                             PublicIdCodec publicIdCodec,
                                             IdResolveProperties properties,
                                             ObjectProvider<List<PublicIdFallbackLookup>> fallbackLookupsProvider,
                                             ObjectProvider<MeterRegistry> meterRegistryProvider) {
        PublicIdL2Cache l2Cache = l2CacheProvider.getIfAvailable();
        List<PublicIdFallbackLookup> fallbackLookups = fallbackLookupsProvider.getIfAvailable(List::of);
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        return new CachedPublicIdResolver(
                repository,
                l2Cache,
                publicIdCodec,
                properties,
                fallbackLookups,
                meterRegistry,
                Clock.systemUTC()
        );
    }

    @Bean
    @ConditionalOnBean(PublicIdMapRepository.class)
    public PublicIdRegistrar publicIdRegistrar(PublicIdMapRepository repository) {
        return new DefaultPublicIdRegistrar(repository);
    }
}
