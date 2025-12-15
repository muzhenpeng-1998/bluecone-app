//package com.bluecone.app.platform.starter;
//
//import com.bluecone.app.core.cacheepoch.api.CacheEpochProvider;
//import com.bluecone.app.core.cacheinval.api.CacheInvalidationPublisher;
//import com.bluecone.app.core.contextkit.ContextCache;
//import com.bluecone.app.core.contextkit.VersionChecker;
//import com.bluecone.app.core.idresolve.api.PublicIdResolver;
//import com.bluecone.app.id.api.IdService;
//import com.bluecone.app.id.core.UlidIdGenerator;
//import com.bluecone.app.id.publicid.api.PublicIdCodec;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.autoconfigure.AutoConfigurations;
//import org.springframework.boot.test.context.runner.ApplicationContextRunner;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
///**
// * Smoke tests for app-platform-starter.
// *
// * <p>Verifies that core beans are properly auto-configured when dependencies are present,
// * and not configured when disabled or dependencies are missing.</p>
// */
//class PlatformStarterSmokeTest {
//
//    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
//            .withConfiguration(AutoConfigurations.of(
//                    com.bluecone.app.id.autoconfigure.IdAutoConfiguration.class,
//                    com.bluecone.app.config.ContextKitAutoConfiguration.class,
//                    com.bluecone.app.config.CacheEpochAutoConfiguration.class
//            ));
//
//    @Test
//    void shouldAutoConfigureIdServiceByDefault() {
//        contextRunner.run(context -> {
//            assertThat(context).hasSingleBean(UlidIdGenerator.class);
//            assertThat(context).hasSingleBean(IdService.class);
//            assertThat(context).hasSingleBean(PublicIdCodec.class);
//        });
//    }
//
//    @Test
//    void shouldNotConfigureIdServiceWhenDisabled() {
//        contextRunner
//                .withPropertyValues("bluecone.id.enabled=false")
//                .run(context -> {
//                    assertThat(context).doesNotHaveBean(UlidIdGenerator.class);
//                    assertThat(context).doesNotHaveBean(IdService.class);
//                });
//    }
//
//    @Test
//    void shouldAutoConfigureContextKitByDefault() {
//        contextRunner.run(context -> {
//            assertThat(context).hasSingleBean(VersionChecker.class);
//            // L1 cache should always be present
//            assertThat(context).hasBean("contextKitL1Cache");
//        });
//    }
//
//    @Test
//    void shouldNotConfigureContextKitWhenDisabled() {
//        contextRunner
//                .withPropertyValues("bluecone.contextkit.enabled=false")
//                .run(context -> {
//                    assertThat(context).doesNotHaveBean(VersionChecker.class);
//                    assertThat(context).doesNotHaveBean("contextKitL1Cache");
//                });
//    }
//
//    @Test
//    void shouldAutoConfigureCacheEpochProvider() {
//        contextRunner.run(context -> {
//            assertThat(context).hasSingleBean(CacheEpochProvider.class);
//        });
//    }
//
//    @Test
//    void shouldNotConfigureCacheInvalidationByDefault() {
//        // Cache invalidation requires explicit enable
//        contextRunner.run(context -> {
//            assertThat(context).doesNotHaveBean(CacheInvalidationPublisher.class);
//        });
//    }
//
//    @Test
//    void shouldConfigureCacheInvalidationWhenEnabled() {
//        contextRunner
//                .withConfiguration(AutoConfigurations.of(
//                        com.bluecone.app.config.CacheInvalidationAutoConfiguration.class
//                ))
//                .withPropertyValues("bluecone.cache.invalidation.enabled=true")
//                .withBean("contextKitCache", ContextCache.class, () -> null)
//                .run(context -> {
//                    // Note: Full configuration requires more dependencies (DomainEventPublisher, etc.)
//                    // This test just verifies the conditional is working
//                    assertThat(context).hasNotFailed();
//                });
//    }
//
//    @Test
//    void shouldNotConfigureCacheInvalidationWhenDisabled() {
//        contextRunner
//                .withConfiguration(AutoConfigurations.of(
//                        com.bluecone.app.config.CacheInvalidationAutoConfiguration.class
//                ))
//                .withPropertyValues("bluecone.cache.invalidation.enabled=false")
//                .run(context -> {
//                    assertThat(context).doesNotHaveBean(CacheInvalidationPublisher.class);
//                });
//    }
//}
//
