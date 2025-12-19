package com.bluecone.app.store.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.store.api.dto.StoreChannelView;
import com.bluecone.app.store.application.command.BindStoreChannelCommand;
import com.bluecone.app.store.application.command.ChangeStoreChannelStatusCommand;
import com.bluecone.app.store.application.command.UnbindStoreChannelCommand;
import com.bluecone.app.store.application.command.UpdateStoreChannelConfigCommand;
import com.bluecone.app.store.application.query.StoreChannelListQuery;
import com.bluecone.app.store.dao.entity.BcStoreChannel;
import com.bluecone.app.store.domain.error.StoreErrorCode;
import com.bluecone.app.store.dao.service.IBcStoreChannelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 门店渠道应用服务。
 * <p>职责：处理「门店与外部渠道绑定」的读写逻辑，保证多租户隔离。</p>
 * <p>后续扩展：可加入重复绑定校验、幂等、事件通知、缓存刷新等。</p>
 */
@Service
public class StoreChannelAppService {

    private final IBcStoreChannelService bcStoreChannelService;
    private final StoreChannelAssembler storeChannelAssembler;

    public StoreChannelAppService(IBcStoreChannelService bcStoreChannelService,
                                  StoreChannelAssembler storeChannelAssembler) {
        this.bcStoreChannelService = bcStoreChannelService;
        this.storeChannelAssembler = storeChannelAssembler;
    }

    public List<StoreChannelView> list(StoreChannelListQuery query) {
        LambdaQueryWrapper<BcStoreChannel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BcStoreChannel::getTenantId, query.getTenantId())
                .eq(BcStoreChannel::getStoreId, query.getStoreId())
                .eq(BcStoreChannel::getIsDeleted, false);
        if (query.getChannelType() != null) {
            wrapper.eq(BcStoreChannel::getChannelType, query.getChannelType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(BcStoreChannel::getStatus, query.getStatus());
        }
        List<BcStoreChannel> list = bcStoreChannelService.list(wrapper);
        return list.stream()
                .map(storeChannelAssembler::toView)
                .collect(Collectors.toList());
    }

    public StoreChannelView getById(Long tenantId, Long storeId, Long channelId) {
        BcStoreChannel entity = bcStoreChannelService.lambdaQuery()
                .eq(BcStoreChannel::getTenantId, tenantId)
                .eq(BcStoreChannel::getStoreId, storeId)
                .eq(BcStoreChannel::getId, channelId)
                .eq(BcStoreChannel::getIsDeleted, false)
                .one();
        if (entity == null) {
            throw new BusinessException(StoreErrorCode.CHANNEL_NOT_FOUND);
        }
        return storeChannelAssembler.toView(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void bindChannel(BindStoreChannelCommand command) {
        BcStoreChannel entity = new BcStoreChannel();
        entity.setTenantId(command.getTenantId());
        entity.setStoreId(command.getStoreId());
        entity.setChannelType(command.getChannelType());
        entity.setExternalStoreId(command.getExternalStoreId());
        entity.setAppId(command.getAppId());
        entity.setConfigJson(command.getConfigJson());
        entity.setStatus("ACTIVE");
        entity.setIsDeleted(false);
        bcStoreChannelService.save(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateChannelConfig(UpdateStoreChannelConfigCommand command) {
        boolean updated = bcStoreChannelService.lambdaUpdate()
                .eq(BcStoreChannel::getTenantId, command.getTenantId())
                .eq(BcStoreChannel::getStoreId, command.getStoreId())
                .eq(BcStoreChannel::getId, command.getChannelId())
                .eq(BcStoreChannel::getIsDeleted, false)
                .set(BcStoreChannel::getConfigJson, command.getConfigJson())
                .update();
        if (!updated) {
            throw new BusinessException(StoreErrorCode.CHANNEL_NOT_FOUND, "更新渠道配置失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(ChangeStoreChannelStatusCommand command) {
        boolean updated = bcStoreChannelService.lambdaUpdate()
                .eq(BcStoreChannel::getTenantId, command.getTenantId())
                .eq(BcStoreChannel::getStoreId, command.getStoreId())
                .eq(BcStoreChannel::getId, command.getChannelId())
                .eq(BcStoreChannel::getIsDeleted, false)
                .set(BcStoreChannel::getStatus, command.getTargetStatus())
                .update();
        if (!updated) {
            throw new BusinessException(StoreErrorCode.CHANNEL_NOT_FOUND, "更新渠道状态失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void unbindChannel(UnbindStoreChannelCommand command) {
        boolean updated = bcStoreChannelService.lambdaUpdate()
                .eq(BcStoreChannel::getTenantId, command.getTenantId())
                .eq(BcStoreChannel::getStoreId, command.getStoreId())
                .eq(BcStoreChannel::getId, command.getChannelId())
                .set(BcStoreChannel::getIsDeleted, true)
                .update();
        if (!updated) {
            throw new BusinessException(StoreErrorCode.CHANNEL_NOT_FOUND, "解绑渠道失败");
        }
    }
}
