package com.bluecone.app.store.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.store.api.dto.StoreResourceView;
import com.bluecone.app.store.application.command.ChangeStoreResourceStatusCommand;
import com.bluecone.app.store.application.command.CreateStoreResourceCommand;
import com.bluecone.app.store.application.command.UpdateStoreResourceCommand;
import com.bluecone.app.store.application.query.StoreResourceListQuery;
import com.bluecone.app.store.dao.entity.BcStoreResource;
import com.bluecone.app.store.domain.error.StoreErrorCode;
import com.bluecone.app.store.dao.service.IBcStoreResourceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 门店资源应用服务（餐桌/包间/场地等）。
 * <p>职责：处理资源的读写与多租户隔离，隐藏实体与 Mapper。</p>
 * <p>扩展点：后续可加入资源占用校验、幂等、事件通知。</p>
 */
@Service
public class StoreResourceAppService {

    private final IBcStoreResourceService bcStoreResourceService;
    private final StoreResourceAssembler storeResourceAssembler;

    public StoreResourceAppService(IBcStoreResourceService bcStoreResourceService,
                                   StoreResourceAssembler storeResourceAssembler) {
        this.bcStoreResourceService = bcStoreResourceService;
        this.storeResourceAssembler = storeResourceAssembler;
    }

    public List<StoreResourceView> list(StoreResourceListQuery query) {
        LambdaQueryWrapper<BcStoreResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BcStoreResource::getTenantId, query.getTenantId())
                .eq(BcStoreResource::getStoreId, query.getStoreId())
                .eq(BcStoreResource::getIsDeleted, false);
        if (query.getResourceType() != null) {
            wrapper.eq(BcStoreResource::getResourceType, query.getResourceType());
        }
        if (query.getArea() != null) {
            wrapper.eq(BcStoreResource::getArea, query.getArea());
        }
        if (query.getStatus() != null) {
            wrapper.eq(BcStoreResource::getStatus, query.getStatus());
        }
        return bcStoreResourceService.list(wrapper).stream()
                .map(storeResourceAssembler::toView)
                .collect(Collectors.toList());
    }

    public StoreResourceView getById(Long tenantId, Long storeId, Long resourceId) {
        BcStoreResource entity = bcStoreResourceService.lambdaQuery()
                .eq(BcStoreResource::getTenantId, tenantId)
                .eq(BcStoreResource::getStoreId, storeId)
                .eq(BcStoreResource::getId, resourceId)
                .eq(BcStoreResource::getIsDeleted, false)
                .one();
        if (entity == null) {
            throw new BizException(StoreErrorCode.RESOURCE_NOT_FOUND);
        }
        return storeResourceAssembler.toView(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(CreateStoreResourceCommand command) {
        BcStoreResource entity = new BcStoreResource();
        entity.setTenantId(command.getTenantId());
        entity.setStoreId(command.getStoreId());
        entity.setResourceType(command.getResourceType());
        entity.setCode(command.getCode());
        entity.setName(command.getName());
        entity.setArea(command.getArea());
        entity.setMetadataJson(command.getMetadataJson());
        entity.setStatus("ENABLED");
        entity.setIsDeleted(false);
        bcStoreResourceService.save(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(UpdateStoreResourceCommand command) {
        boolean updated = bcStoreResourceService.lambdaUpdate()
                .eq(BcStoreResource::getTenantId, command.getTenantId())
                .eq(BcStoreResource::getStoreId, command.getStoreId())
                .eq(BcStoreResource::getId, command.getResourceId())
                .eq(BcStoreResource::getIsDeleted, false)
                .set(command.getName() != null, BcStoreResource::getName, command.getName())
                .set(command.getArea() != null, BcStoreResource::getArea, command.getArea())
                .set(command.getMetadataJson() != null, BcStoreResource::getMetadataJson, command.getMetadataJson())
                .update();
        if (!updated) {
            throw new BizException(StoreErrorCode.RESOURCE_NOT_FOUND, "更新门店资源失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(ChangeStoreResourceStatusCommand command) {
        boolean updated = bcStoreResourceService.lambdaUpdate()
                .eq(BcStoreResource::getTenantId, command.getTenantId())
                .eq(BcStoreResource::getStoreId, command.getStoreId())
                .eq(BcStoreResource::getId, command.getResourceId())
                .eq(BcStoreResource::getIsDeleted, false)
                .set(BcStoreResource::getStatus, command.getTargetStatus())
                .update();
        if (!updated) {
            throw new BizException(StoreErrorCode.RESOURCE_NOT_FOUND, "调整资源状态失败");
        }
    }
}
