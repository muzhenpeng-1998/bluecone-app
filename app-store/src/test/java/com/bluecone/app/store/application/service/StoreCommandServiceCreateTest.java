package com.bluecone.app.store.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bluecone.app.core.domain.IndustryType;
import com.bluecone.app.core.idresolve.api.PublicIdRegistrar;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import com.bluecone.app.store.application.command.CreateStoreCommand;
import com.bluecone.app.store.dao.entity.BcStore;
import com.bluecone.app.store.application.service.StoreCacheInvalidator;
import com.bluecone.app.store.dao.service.IBcStoreCapabilityService;
import com.bluecone.app.store.dao.service.IBcStoreOpeningHoursService;
import com.bluecone.app.store.dao.service.IBcStoreService;
import com.bluecone.app.store.dao.service.IBcStoreSpecialDayService;
import com.bluecone.app.store.domain.repository.StoreRepository;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * StoreCommandService 创建门店 ID 写入行为测试。
 */
class StoreCommandServiceCreateTest {

    @Test
    void createStoreWithPreallocatedIdsShouldWriteInternalPublicAndStoreNo() {
        IBcStoreService storeService = mock(IBcStoreService.class);
        IBcStoreCapabilityService capabilityService = mock(IBcStoreCapabilityService.class);
        IBcStoreOpeningHoursService openingHoursService = mock(IBcStoreOpeningHoursService.class);
        IBcStoreSpecialDayService specialDayService = mock(IBcStoreSpecialDayService.class);
        StoreConfigChangeService storeConfigChangeService = mock(StoreConfigChangeService.class);
        StoreRepository storeRepository = mock(StoreRepository.class);
        StoreCacheInvalidator storeCacheInvalidator = mock(StoreCacheInvalidator.class);
        IdService idService = mock(IdService.class);
        PublicIdCodec publicIdCodec = mock(PublicIdCodec.class);
        PublicIdRegistrar publicIdRegistrar = mock(PublicIdRegistrar.class);

        AtomicReference<BcStore> saved = new AtomicReference<>();
        when(storeService.save(any(BcStore.class))).thenAnswer(invocation -> {
            BcStore entity = invocation.getArgument(0);
            entity.setId(1L);
            saved.set(entity);
            return true;
        });

        StoreCommandService service = new StoreCommandService(
                storeService,
                capabilityService,
                openingHoursService,
                specialDayService,
                storeConfigChangeService,
                storeRepository,
                storeCacheInvalidator,
                idService,
                publicIdCodec,
                publicIdRegistrar
        );

        CreateStoreCommand command = new CreateStoreCommand();
        command.setTenantId(1001L);
        command.setName("Test Store");
        command.setIndustryType(IndustryType.COFFEE);

        Ulid128 internalId = new Ulid128(1L, 2L);
        String publicId = "sto_0123456789abcdefghijklmnopqr";
        Long storeNo = 123456789L;

        service.createStoreWithPreallocatedIds(command, internalId, publicId, storeNo);

        BcStore entity = saved.get();
        assertNotNull(entity, "应写入一条门店记录");
        assertEquals(internalId, entity.getInternalId(), "internal_id 应来自模板传入的 Ulid128");
        assertEquals(publicId, entity.getPublicId(), "public_id 应来自模板传入的字符串");
        assertEquals(storeNo, entity.getStoreNo(), "store_no 应写入 Snowflake long 编号");
        assertEquals(command.getTenantId(), entity.getTenantId(), "tenantId 应保持一致");
    }
}
