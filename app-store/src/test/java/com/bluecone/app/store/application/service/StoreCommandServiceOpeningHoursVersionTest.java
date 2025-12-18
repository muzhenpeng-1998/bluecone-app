package com.bluecone.app.store.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bluecone.app.core.idresolve.api.PublicIdRegistrar;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import com.bluecone.app.store.application.command.UpdateStoreOpeningHoursCommand;
import com.bluecone.app.store.dao.service.IBcStoreCapabilityService;
import com.bluecone.app.store.dao.service.IBcStoreOpeningHoursService;
import com.bluecone.app.store.dao.service.IBcStoreService;
import com.bluecone.app.store.dao.service.IBcStoreSpecialDayService;
import com.bluecone.app.store.domain.model.StoreOpeningSchedule;
import com.bluecone.app.store.domain.repository.StoreRepository;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * StoreCommandService 更新营业时间后版本递增测试。
 * <p>验证：更新营业时间后，configVersion 必须递增。</p>
 */
class StoreCommandServiceOpeningHoursVersionTest {

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
    void updateOpeningHoursShouldIncrementConfigVersion() {
        // 准备数据
        Long tenantId = 1001L;
        Long storeId = 1L;
        Long expectedVersion = 1L;
        Long newVersion = 2L;

        // 构造营业时间配置
        List<StoreOpeningSchedule.OpeningHoursItem> regularHours = new ArrayList<>();
        regularHours.add(StoreOpeningSchedule.OpeningHoursItem.builder()
                .weekday(1)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .periodType("REGULAR")
                .build());

        StoreOpeningSchedule schedule = StoreOpeningSchedule.builder()
                .regularHours(regularHours)
                .specialDays(null)
                .build();

        UpdateStoreOpeningHoursCommand command = new UpdateStoreOpeningHoursCommand();
        command.setTenantId(tenantId);
        command.setStoreId(storeId);
        command.setExpectedConfigVersion(expectedVersion);
        command.setSchedule(schedule);

        // 模拟仓储行为
        when(storeRepository.getConfigVersion(tenantId, storeId)).thenReturn(expectedVersion);
        when(storeRepository.bumpConfigVersion(tenantId, storeId, expectedVersion)).thenReturn(newVersion);

        // 执行更新
        storeCommandService.updateOpeningHours(command);

        // 验证：版本应该递增
        verify(storeRepository, times(1)).getConfigVersion(tenantId, storeId);
        verify(storeRepository, times(1)).updateOpeningSchedule(tenantId, storeId, schedule);
        verify(storeRepository, times(1)).bumpConfigVersion(tenantId, storeId, expectedVersion);
        
        // 验证：配置变更通知应该使用新版本
        verify(storeConfigChangeService, times(1)).onStoreConfigChanged(tenantId, storeId, newVersion);
        
        // 验证：缓存失效应该使用新版本
        verify(storeCacheInvalidator, times(1)).invalidateStoreBase(tenantId, storeId);
        verify(storeCacheInvalidator, times(1)).invalidateStoreConfig(tenantId, storeId, newVersion);
    }

    @Test
    void updateOpeningHoursMultipleTimesShouldIncrementVersionEachTime() {
        // 准备数据
        Long tenantId = 1001L;
        Long storeId = 1L;

        // 第一次更新：版本从 1 变为 2
        Long version1 = 1L;
        Long version2 = 2L;

        List<StoreOpeningSchedule.OpeningHoursItem> regularHours1 = new ArrayList<>();
        regularHours1.add(StoreOpeningSchedule.OpeningHoursItem.builder()
                .weekday(1)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .periodType("REGULAR")
                .build());

        StoreOpeningSchedule schedule1 = StoreOpeningSchedule.builder()
                .regularHours(regularHours1)
                .specialDays(null)
                .build();

        UpdateStoreOpeningHoursCommand command1 = new UpdateStoreOpeningHoursCommand();
        command1.setTenantId(tenantId);
        command1.setStoreId(storeId);
        command1.setExpectedConfigVersion(version1);
        command1.setSchedule(schedule1);

        when(storeRepository.getConfigVersion(tenantId, storeId)).thenReturn(version1);
        when(storeRepository.bumpConfigVersion(tenantId, storeId, version1)).thenReturn(version2);

        storeCommandService.updateOpeningHours(command1);

        // 第二次更新：版本从 2 变为 3
        Long version3 = 3L;

        List<StoreOpeningSchedule.OpeningHoursItem> regularHours2 = new ArrayList<>();
        regularHours2.add(StoreOpeningSchedule.OpeningHoursItem.builder()
                .weekday(1)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(20, 0))
                .periodType("REGULAR")
                .build());

        StoreOpeningSchedule schedule2 = StoreOpeningSchedule.builder()
                .regularHours(regularHours2)
                .specialDays(null)
                .build();

        UpdateStoreOpeningHoursCommand command2 = new UpdateStoreOpeningHoursCommand();
        command2.setTenantId(tenantId);
        command2.setStoreId(storeId);
        command2.setExpectedConfigVersion(version2);
        command2.setSchedule(schedule2);

        when(storeRepository.getConfigVersion(tenantId, storeId)).thenReturn(version2);
        when(storeRepository.bumpConfigVersion(tenantId, storeId, version2)).thenReturn(version3);

        storeCommandService.updateOpeningHours(command2);

        // 验证：版本应该递增两次
        verify(storeRepository, times(2)).getConfigVersion(tenantId, storeId);
        verify(storeRepository, times(2)).updateOpeningSchedule(eq(tenantId), eq(storeId), any(StoreOpeningSchedule.class));
        verify(storeRepository, times(1)).bumpConfigVersion(tenantId, storeId, version1);
        verify(storeRepository, times(1)).bumpConfigVersion(tenantId, storeId, version2);
        
        // 验证：配置变更通知应该使用新版本
        verify(storeConfigChangeService, times(1)).onStoreConfigChanged(tenantId, storeId, version2);
        verify(storeConfigChangeService, times(1)).onStoreConfigChanged(tenantId, storeId, version3);
    }
}
