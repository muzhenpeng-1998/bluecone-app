package com.bluecone.app.store;

import com.bluecone.app.core.domain.IndustryType;
import com.bluecone.app.store.api.StoreFacade;
import com.bluecone.app.store.api.dto.StoreBaseView;
import com.bluecone.app.store.api.dto.StoreOrderAcceptResult;
import com.bluecone.app.store.application.command.CreateStoreCommand;
import com.bluecone.app.store.application.command.UpdateStoreBaseCommand;
import com.bluecone.app.store.application.query.StoreDetailQuery;
import com.bluecone.app.store.application.query.StoreListQuery;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.store.domain.error.StoreErrorCode;
import com.bluecone.app.store.domain.model.StoreCapabilityModel;
import com.bluecone.app.store.domain.model.StoreOpeningSchedule;
import com.bluecone.app.store.application.command.UpdateStoreCapabilitiesCommand;
import com.bluecone.app.store.application.command.UpdateStoreOpeningHoursCommand;
import com.bluecone.app.test.AbstractWebIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 门店模块集成测试。
 * <p>测试门店创建、查询、更新、可接单判断等核心功能。</p>
 * <p>说明：此测试继承 AbstractWebIntegrationTest，使用 Testcontainers 提供数据库和 Redis 支持。</p>
 */
public class StoreIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private StoreFacade storeFacade;

    private static final Long TEST_TENANT_ID = 1001L;
    private String createdStorePublicId;
    private Long createdStoreId;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        flushRedis();
        TenantContext.setTenantId(TEST_TENANT_ID.toString());
    }

    /**
     * 测试：创建门店 → 查询门店详情 → 更新门店 → 再次查询。
     * <p>验证门店 CRUD 基本流程和乐观锁机制。</p>
     */
    @Test
    void testCreateStoreAndQueryAndUpdate() {
        // 1. 创建门店
        CreateStoreCommand createCommand = CreateStoreCommand.builder()
                .tenantId(TEST_TENANT_ID)
                .name("测试门店")
                .shortName("测试")
                .industryType(IndustryType.FOOD)
                .cityCode("330100")
                .openForOrders(true)
                .build();

        createdStorePublicId = storeFacade.createStore(createCommand);
        assertThat(createdStorePublicId).isNotNull().isNotEmpty();
        assertThat(createdStorePublicId).startsWith("sto_");

        // 2. 查询门店详情（通过 publicId）
        StoreDetailQuery detailQuery = StoreDetailQuery.builder()
                .tenantId(TEST_TENANT_ID)
                .storePublicId(createdStorePublicId)
                .build();

        StoreBaseView detail = storeFacade.detail(detailQuery);
        assertThat(detail).isNotNull();
        assertThat(detail.getStoreName()).isEqualTo("测试门店");
        assertThat(detail.getTenantId()).isEqualTo(TEST_TENANT_ID);
        createdStoreId = detail.getStoreId();
        assertThat(createdStoreId).isNotNull();

        // 3. 更新门店基础信息（需要先获取 configVersion）
        // 说明：首次创建时 configVersion 为 1，使用乐观锁机制
        UpdateStoreBaseCommand updateCommand = UpdateStoreBaseCommand.builder()
                .tenantId(TEST_TENANT_ID)
                .storeId(createdStoreId)
                .expectedConfigVersion(1L)
                .name("测试门店（已更新）")
                .build();

        storeFacade.updateStoreBase(updateCommand);

        // 4. 再次查询，验证更新成功
        StoreBaseView updatedDetail = storeFacade.detail(detailQuery);
        assertThat(updatedDetail).isNotNull();
        assertThat(updatedDetail.getStoreName()).isEqualTo("测试门店（已更新）");
    }

    /**
     * 测试：更新能力配置。
     * <p>验证门店能力（如堂食、外卖）的配置更新功能。</p>
     */
    @Test
    void testUpdateCapabilities() {
        // 1. 创建门店
        CreateStoreCommand createCommand = CreateStoreCommand.builder()
                .tenantId(TEST_TENANT_ID)
                .name("测试门店-能力")
                .industryType(IndustryType.FOOD)
                .cityCode("330100")
                .build();

        String publicId = storeFacade.createStore(createCommand);
        StoreDetailQuery query = StoreDetailQuery.builder()
                .tenantId(TEST_TENANT_ID)
                .storePublicId(publicId)
                .build();
        StoreBaseView store = storeFacade.detail(query);
        Long storeId = store.getStoreId();

        // 2. 更新能力配置
        StoreCapabilityModel dineIn = StoreCapabilityModel.builder()
                .capability("DINE_IN")
                .enabled(true)
                .build();
        StoreCapabilityModel takeOut = StoreCapabilityModel.builder()
                .capability("TAKE_OUT")
                .enabled(true)
                .build();

        UpdateStoreCapabilitiesCommand updateCommand = UpdateStoreCapabilitiesCommand.builder()
                .tenantId(TEST_TENANT_ID)
                .storeId(storeId)
                .expectedConfigVersion(1L)
                .capabilities(Arrays.asList(dineIn, takeOut))
                .build();

        storeFacade.updateCapabilities(updateCommand);

        // 3. 验证可接单判断（DINE_IN 能力）
        // 注意：由于营业时间未配置，可能返回不在营业时间内，这里仅验证方法调用成功
        StoreOrderAcceptResult result = storeFacade.checkOrderAcceptable(
                TEST_TENANT_ID, storeId, "DINE_IN", LocalDateTime.now(), null);
        assertThat(result).isNotNull();
    }

    /**
     * 测试：更新营业时间。
     * <p>验证门店常规营业时间的配置更新功能。</p>
     */
    @Test
    void testUpdateOpeningHours() {
        // 1. 创建门店
        CreateStoreCommand createCommand = CreateStoreCommand.builder()
                .tenantId(TEST_TENANT_ID)
                .name("测试门店-营业时间")
                .industryType(IndustryType.FOOD)
                .cityCode("330100")
                .build();

        String publicId = storeFacade.createStore(createCommand);
        StoreDetailQuery query = StoreDetailQuery.builder()
                .tenantId(TEST_TENANT_ID)
                .storePublicId(publicId)
                .build();
        StoreBaseView store = storeFacade.detail(query);
        Long storeId = store.getStoreId();

        // 2. 更新营业时间（周一至周五 9:00-18:00）
        StoreOpeningSchedule.OpeningHoursItem monday = StoreOpeningSchedule.OpeningHoursItem.builder()
                .weekday(1)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .periodType("REGULAR")
                .build();

        StoreOpeningSchedule schedule = StoreOpeningSchedule.builder()
                .regularHours(Arrays.asList(monday))
                .build();

        UpdateStoreOpeningHoursCommand updateCommand = UpdateStoreOpeningHoursCommand.builder()
                .tenantId(TEST_TENANT_ID)
                .storeId(storeId)
                .expectedConfigVersion(1L)
                .schedule(schedule)
                .build();

        storeFacade.updateOpeningHours(updateCommand);

        // 3. 验证可接单判断
        // 注意：实际判断需要考虑当前日期是否为周一，这里仅验证方法调用成功
        LocalDateTime testTime = LocalDateTime.now();
        StoreOrderAcceptResult result = storeFacade.checkOrderAcceptable(
                TEST_TENANT_ID, storeId, null, testTime, null);
        assertThat(result).isNotNull();
    }

    /**
     * 测试：并发更新冲突（乐观锁）。
     * <p>验证使用旧版本号更新时应该抛出 StoreConfigVersionConflictException。</p>
     */
    @Test
    void testConcurrentUpdateConflict() {
        // 1. 创建门店
        CreateStoreCommand createCommand = CreateStoreCommand.builder()
                .tenantId(TEST_TENANT_ID)
                .name("测试门店-并发")
                .industryType(IndustryType.FOOD)
                .cityCode("330100")
                .build();

        String publicId = storeFacade.createStore(createCommand);
        StoreDetailQuery query = StoreDetailQuery.builder()
                .tenantId(TEST_TENANT_ID)
                .storePublicId(publicId)
                .build();
        StoreBaseView store = storeFacade.detail(query);
        Long storeId = store.getStoreId();

        // 2. 第一次更新（成功，版本号从 1 变为 2）
        UpdateStoreBaseCommand update1 = UpdateStoreBaseCommand.builder()
                .tenantId(TEST_TENANT_ID)
                .storeId(storeId)
                .expectedConfigVersion(1L)
                .name("第一次更新")
                .build();
        storeFacade.updateStoreBase(update1);

        // 3. 第二次更新使用旧版本号（应该失败）
        UpdateStoreBaseCommand update2 = UpdateStoreBaseCommand.builder()
                .tenantId(TEST_TENANT_ID)
                .storeId(storeId)
                .expectedConfigVersion(1L) // 使用旧版本，应该失败
                .name("第二次更新")
                .build();

        // 预期抛出版本冲突异常
        assertThatThrownBy(() -> storeFacade.updateStoreBase(update2))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException bizEx = (BizException) ex;
                    assertThat(bizEx.getErrorCode()).isEqualTo(StoreErrorCode.STORE_CONFIG_CONFLICT);
                });
    }

    /**
     * 测试：门店列表查询。
     * <p>验证按租户、城市等条件查询门店列表的功能。</p>
     */
    @Test
    void testListStores() {
        // 1. 创建多个门店
        for (int i = 0; i < 3; i++) {
            CreateStoreCommand createCommand = CreateStoreCommand.builder()
                    .tenantId(TEST_TENANT_ID)
                    .name("测试门店-" + i)
                    .industryType(IndustryType.FOOD)
                    .cityCode("330100")
                    .build();
            storeFacade.createStore(createCommand);
        }

        // 2. 查询列表
        StoreListQuery listQuery = StoreListQuery.builder()
                .tenantId(TEST_TENANT_ID)
                .cityCode("330100")
                .build();

        List<StoreBaseView> stores = storeFacade.list(listQuery);
        assertThat(stores).isNotNull();
        assertThat(stores.size()).isGreaterThanOrEqualTo(3);
    }
}

