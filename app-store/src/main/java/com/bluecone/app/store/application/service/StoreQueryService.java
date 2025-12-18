package com.bluecone.app.store.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.store.api.dto.StoreBaseView;
import com.bluecone.app.store.application.query.StoreDetailQuery;
import com.bluecone.app.store.application.query.StoreListQuery;
import com.bluecone.app.store.dao.entity.BcStore;
import com.bluecone.app.store.dao.service.IBcStoreService;
import com.bluecone.app.store.domain.model.StoreConfig;
import com.bluecone.app.store.application.service.StoreConfigService;
import com.bluecone.app.store.application.service.StoreResourceService;
import com.bluecone.app.store.domain.service.assembler.StoreViewAssembler;
import com.bluecone.app.store.domain.error.StoreErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 店铺查询应用服务，封装读侧用例。
 * <p>高隔离：仅暴露 DTO，内部复用领域仓储，不让上层感知 Mapper。</p>
 * <p>高稳定：后续可在此加入缓存/降级逻辑，当前直连 DB 以打通调用链。</p>
 */
@Service
public class StoreQueryService {

    private final IBcStoreService bcStoreService;
    private final StoreConfigService storeConfigService;
    private final StoreViewAssembler storeViewAssembler;
    private final StoreResourceService storeResourceService;

    public StoreQueryService(IBcStoreService bcStoreService,
                             StoreConfigService storeConfigService,
                             StoreViewAssembler storeViewAssembler,
                             StoreResourceService storeResourceService) {
        this.bcStoreService = bcStoreService;
        this.storeConfigService = storeConfigService;
        this.storeViewAssembler = storeViewAssembler;
        this.storeResourceService = storeResourceService;
    }

    /**
     * 列表查询门店基础信息。
     * <p>支持按租户、城市、行业类型、状态、关键字等条件筛选。</p>
     * <p>说明：当前实现未分页，后续可扩展分页功能。</p>
     *
     * @param query 查询条件（包含 tenantId、cityCode、industryType、status、keyword 等）
     * @return 门店基础信息视图列表
     */
    public List<StoreBaseView> listStores(StoreListQuery query) {
        LambdaQueryWrapper<BcStore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BcStore::getTenantId, query.getTenantId());
        wrapper.eq(BcStore::getIsDeleted, false);
        if (query.getCityCode() != null) {
            wrapper.eq(BcStore::getCityCode, query.getCityCode());
        }
        if (query.getIndustryType() != null) {
            wrapper.eq(BcStore::getIndustryType, query.getIndustryType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(BcStore::getStatus, query.getStatus());
        }
        if (query.getKeyword() != null) {
            wrapper.and(w -> w.like(BcStore::getName, query.getKeyword())
                    .or().like(BcStore::getShortName, query.getKeyword()));
        }
        // 这里先不分页，后续可扩展
        List<BcStore> entities = bcStoreService.list(wrapper);
        // 映射为 StoreBaseView，过滤掉 null 值（确保不返回 null）
        // 字段说明：
        // - tenantId: 租户 ID，用于多租户隔离
        // - storeId: 门店内部 ID（自增主键）
        // - storeCode: 门店编码，通常对外展示使用
        // - name/shortName: 门店名称/简称
        // - industryType: 行业类型（餐饮、零售等）
        // - cityCode: 城市编码，用于区域相关业务
        // - status: 业务状态（OPEN/PAUSED/CLOSED）
        // - openForOrders: 是否可接单（配置维度开关）
        // - logoUrl/coverUrl: 门店 Logo 和封面图 URL
        return entities.stream()
                .map(storeViewAssembler::toStoreBaseView)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 单个门店详情查询。
     * <p>支持通过 storePublicId、storeId 或 storeCode 查询门店详情。</p>
     * <p>说明：查询优先级为 storePublicId > storeId > storeCode，至少需要提供其中一个标识。</p>
     *
     * @param query 查询条件（必须包含 tenantId 和至少一个门店标识：storePublicId/storeId/storeCode）
     * @return 门店基础信息视图
     * @throws BizException 当门店不存在或查询条件不满足时抛出
     */
    public StoreBaseView getStoreDetail(StoreDetailQuery query) {
        LambdaQueryWrapper<BcStore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BcStore::getTenantId, query.getTenantId());
        wrapper.eq(BcStore::getIsDeleted, false);
        if (query.getStorePublicId() != null && !query.getStorePublicId().isBlank()) {
            wrapper.eq(BcStore::getPublicId, query.getStorePublicId());
        } else if (query.getStoreId() != null) {
            wrapper.eq(BcStore::getId, query.getStoreId());
        } else if (query.getStoreCode() != null) {
            wrapper.eq(BcStore::getStoreCode, query.getStoreCode());
        } else {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "需要提供 storePublicId、storeId 或 storeCode");
        }
        BcStore entity = bcStoreService.getOne(wrapper, false);
        if (entity == null) {
            throw new BizException(StoreErrorCode.STORE_NOT_FOUND);
        }
        StoreBaseView view = storeViewAssembler.toStoreBaseView(entity);
        // 确保不返回 null：如果装配器返回 null，抛出异常
        if (view == null) {
            throw new BizException(StoreErrorCode.STORE_NOT_FOUND, "门店数据装配失败");
        }
        // 补充 Logo URL（如果存在）
        String logoUrl = storeResourceService.resolveStoreLogoUrl(entity.getId());
        if (logoUrl != null) {
            view.setLogoUrl(logoUrl);
        }
        // 字段说明：
        // - tenantId: 租户 ID，用于多租户隔离
        // - storeId: 门店内部 ID（自增主键）
        // - storeCode: 门店编码，通常对外展示使用
        // - name/shortName: 门店名称/简称
        // - industryType: 行业类型（餐饮、零售等）
        // - cityCode: 城市编码，用于区域相关业务
        // - status: 业务状态（OPEN/PAUSED/CLOSED）
        // - openForOrders: 是否可接单（配置维度开关）
        // - logoUrl/coverUrl: 门店 Logo 和封面图 URL
        return view;
    }

    /**
     * 供 Facade/订单模块使用的聚合加载入口。
     * <p>加载完整的门店配置聚合（包含能力、营业时间、渠道等），供订单模块判断是否可接单。</p>
     * <p>高并发：后续可在此包装多级缓存；当前直连仓储。</p>
     * <p>说明：此方法会先查询门店的 configVersion，再加载对应版本的完整配置，确保版本一致性。</p>
     *
     * @param tenantId 租户 ID（不能为空）
     * @param storeId  门店 ID（不能为空）
     * @return 门店完整配置聚合（StoreConfig）
     * @throws BizException 当门店不存在时抛出 STORE_NOT_FOUND
     */
    public StoreConfig loadStoreConfig(Long tenantId, Long storeId) {
        Objects.requireNonNull(tenantId, "tenantId 不能为空");
        Objects.requireNonNull(storeId, "storeId 不能为空");
        BcStore entity = bcStoreService.lambdaQuery()
                .eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getId, storeId)
                .eq(BcStore::getIsDeleted, false)
                .select(BcStore::getConfigVersion)
                .one();
        if (entity == null) {
            throw new BizException(StoreErrorCode.STORE_NOT_FOUND);
        }
        return storeConfigService.loadConfig(tenantId, storeId, entity.getConfigVersion());
    }
}
