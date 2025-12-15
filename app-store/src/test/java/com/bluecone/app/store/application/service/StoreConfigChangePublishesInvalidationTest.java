package com.bluecone.app.store.application.service;

import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;
import com.bluecone.app.core.cacheinval.api.CacheInvalidationPublisher;
import com.bluecone.app.core.cacheinval.api.InvalidationScope;
import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.core.event.EventMetadata;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.infra.cache.facade.CacheClient;
import com.bluecone.app.infra.cache.profile.CacheProfileRegistry;
import com.bluecone.app.store.dao.entity.BcStore;
import com.bluecone.app.store.dao.service.IBcStoreService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreConfigChangePublishesInvalidationTest {

    @Test
    void shouldPublishStoreSnapshotInvalidationEvent() {
        CacheClient cacheClient = mock(CacheClient.class);
        CacheProfileRegistry profileRegistry = mock(CacheProfileRegistry.class);
        StoreConfigService storeConfigService = mock(StoreConfigService.class);
        DomainEventPublisher domainEventPublisher = mock(DomainEventPublisher.class);
        IBcStoreService bcStoreService = mock(IBcStoreService.class);
        IdService idService = mock(IdService.class);
        CacheInvalidationPublisher cacheInvalidationPublisher = mock(CacheInvalidationPublisher.class);

        StoreConfigChangeServiceImpl service = new StoreConfigChangeServiceImpl(
                cacheClient,
                profileRegistry,
                storeConfigService,
                domainEventPublisher,
                bcStoreService,
                idService,
                cacheInvalidationPublisher
        );

        Long tenantId = 1L;
        Long storeId = 10L;
        Long newVersion = 2L;
        Ulid128 internalId = new Ulid128(1L, 2L);

        BcStore store = new BcStore();
        store.setId(storeId);
        store.setTenantId(tenantId);
        store.setInternalId(internalId);
        when(bcStoreService.getById(storeId)).thenReturn(store);
        when(idService.nextUlidString()).thenReturn("evt-123");

        service.onStoreConfigChanged(tenantId, storeId, newVersion);

        verify(cacheInvalidationPublisher).publishAfterCommit(any(CacheInvalidationEvent.class));
    }
}

