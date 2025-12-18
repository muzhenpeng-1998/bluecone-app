package com.bluecone.app.store.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bluecone.app.core.domain.IndustryType;
import com.bluecone.app.core.idresolve.api.PublicIdRegistrar;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import com.bluecone.app.store.application.command.UpdateStoreBaseCommand;
import com.bluecone.app.store.dao.service.IBcStoreCapabilityService;
import com.bluecone.app.store.dao.service.IBcStoreOpeningHoursService;
import com.bluecone.app.store.dao.service.IBcStoreService;
import com.bluecone.app.store.dao.service.IBcStoreSpecialDayService;
import com.bluecone.app.store.domain.exception.StoreConfigVersionConflictException;
import com.bluecone.app.store.domain.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * StoreCommandService 并发更新版本冲突测试。
 * <p>验证：当两次更新使用相同的 expectedConfigVersion 时，第二次更新必须失败并抛出版本冲突异常。</p>
 */
class StoreCommandServiceVersionConflictTest {

    private IBcStoreService bcStoreService;
    private IBcStoreCapabilityService bcStoreCapabilityService;
    private IBcStoreOpeningHoursService bcStoreOpeningHoursService;
    private IBcStoreSpecialDayService bcStoreSpecialDayService;
    private StoreConfigChangeService storeConfigChangeService;
    private StoreRepository storeRepository;
    private StoreCacheInvalidator storeCacheInvalidator;
    private IdService idService;
    private PublicIdCodec publicIdCodec;
    private PublicIdRegistrar publicIdRegistrar;
    private StoreCommandService storeCommandService;

    @BeforeEach
    void setUp() {
        bcStoreService = mock(IBcStoreService.class);
        bcStoreCapabilityService = mock(IBcStoreCapabilityService.class);
        bcStoreOpeningHoursService = mock(IBcStoreOpeningHoursService.class);
        bcStoreSpecialDayService = mock(IBcStoreSpecialDayService.class);
        storeConfigChangeService = mock(StoreConfigChangeService.class);
        storeRepository = mock(StoreRepository.class);
        storeCacheInvalidator = mock(StoreCacheInvalidator.class);
        idService = mock(IdService.class);
        publicIdCodec = mock(PublicIdCodec.class);
        publicIdRegistrar = mock(PublicIdRegistrar.class);

        storeCommandService = new StoreCommandService(
                bcStoreService,
                bcStoreCapabilityService,
                bcStoreOpeningHoursService,
                bcStoreSpecialDayService,
                storeConfigChangeService,
                storeRepository,
                storeCacheInvalidator,
                idService,
                publicIdCodec,
                publicIdRegistrar
        );
    }

    @Test
    void updateStoreBaseShouldFailWhenVersionConflict() {
        // 准备数据
        Long tenantId = 1001L;
        Long storeId = 1L;
        Long expectedVersion = 1L;

        UpdateStoreBaseCommand command = new UpdateStoreBaseCommand();
        command.setTenantId(tenantId);
        command.setStoreId(storeId);
        command.setExpectedConfigVersion(expectedVersion);
        command.setName("Updated Store Name");

        // 模拟第一次更新成功：当前版本为 1，更新后版本变为 2
        when(storeRepository.getConfigVersion(tenantId, storeId)).thenReturn(1L);
        when(bcStoreService.update(any())).thenReturn(true);

        // 第一次更新应该成功
        storeCommandService.updateStoreBase(command);

        // 验证第一次更新成功
        verify(storeRepository, times(1)).getConfigVersion(tenantId, storeId);
        verify(bcStoreService, times(1)).update(any());
        verify(storeConfigChangeService, times(1)).onStoreConfigChanged(tenantId, storeId, 2L);

        // 模拟第二次更新：当前版本已经是 2，但命令中期望版本仍然是 1
        when(storeRepository.getConfigVersion(tenantId, storeId)).thenReturn(2L);

        // 第二次更新应该失败，抛出版本冲突异常
        assertThrows(StoreConfigVersionConflictException.class, () -> {
            storeCommandService.updateStoreBase(command);
        }, "第二次更新应该失败，因为版本不匹配");

        // 验证第二次更新没有执行数据库更新
        verify(bcStoreService, times(1)).update(any()); // 仍然是 1 次，说明第二次没有执行
        verify(storeConfigChangeService, times(1)).onStoreConfigChanged(tenantId, storeId, 2L); // 仍然是 1 次
    }

    @Test
    void concurrentUpdateWithSameExpectedVersionShouldFail() {
        // 准备数据：两个并发请求使用相同的 expectedVersion
        Long tenantId = 1001L;
        Long storeId = 1L;
        Long expectedVersion = 1L;

        UpdateStoreBaseCommand command1 = new UpdateStoreBaseCommand();
        command1.setTenantId(tenantId);
        command1.setStoreId(storeId);
        command1.setExpectedConfigVersion(expectedVersion);
        command1.setName("Updated Name 1");

        UpdateStoreBaseCommand command2 = new UpdateStoreBaseCommand();
        command2.setTenantId(tenantId);
        command2.setStoreId(storeId);
        command2.setExpectedConfigVersion(expectedVersion);
        command2.setName("Updated Name 2");

        // 模拟第一个请求：当前版本为 1，更新成功
        when(storeRepository.getConfigVersion(tenantId, storeId)).thenReturn(1L);
        when(bcStoreService.update(any())).thenReturn(true);

        // 第一个请求成功
        storeCommandService.updateStoreBase(command1);

        // 模拟第二个请求：当前版本已经是 2（因为第一个请求已经更新），但期望版本仍然是 1
        when(storeRepository.getConfigVersion(tenantId, storeId)).thenReturn(2L);

        // 第二个请求应该失败
        assertThrows(StoreConfigVersionConflictException.class, () -> {
            storeCommandService.updateStoreBase(command2);
        }, "并发更新应该失败，因为版本已被第一个请求修改");
    }
}
