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
        return entities.stream()
                .map(storeViewAssembler::toStoreBaseView)
                .collect(Collectors.toList());
    }

    /**
     * 单个门店详情。
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
        if (view != null) {
            String logoUrl = storeResourceService.resolveStoreLogoUrl(entity.getId());
            if (logoUrl != null) {
                view.setLogoUrl(logoUrl);
            }
        }
        return view;
    }

    /**
     * 供 Facade/订单模块使用的聚合加载入口。
     * <p>高并发：后续可在此包装多级缓存；当前直连仓储。</p>
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
