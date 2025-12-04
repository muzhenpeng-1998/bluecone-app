package com.bluecone.app.store.api.impl;

import com.bluecone.app.store.api.StoreChannelFacade;
import com.bluecone.app.store.api.dto.StoreChannelView;
import com.bluecone.app.store.application.command.BindStoreChannelCommand;
import com.bluecone.app.store.application.command.ChangeStoreChannelStatusCommand;
import com.bluecone.app.store.application.command.UnbindStoreChannelCommand;
import com.bluecone.app.store.application.command.UpdateStoreChannelConfigCommand;
import com.bluecone.app.store.application.query.StoreChannelListQuery;
import com.bluecone.app.store.application.service.StoreChannelAppService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门店渠道 Facade 实现。
 * <p>职责：承接外部调用并委派给应用服务，不做复杂逻辑。</p>
 */
@Service
public class StoreChannelFacadeImpl implements StoreChannelFacade {

    private final StoreChannelAppService appService;

    public StoreChannelFacadeImpl(StoreChannelAppService appService) {
        this.appService = appService;
    }

    @Override
    public List<StoreChannelView> list(StoreChannelListQuery query) {
        return appService.list(query);
    }

    @Override
    public StoreChannelView getById(Long tenantId, Long storeId, Long channelId) {
        return appService.getById(tenantId, storeId, channelId);
    }

    @Override
    public void bindChannel(BindStoreChannelCommand command) {
        appService.bindChannel(command);
    }

    @Override
    public void updateChannelConfig(UpdateStoreChannelConfigCommand command) {
        appService.updateChannelConfig(command);
    }

    @Override
    public void changeChannelStatus(ChangeStoreChannelStatusCommand command) {
        appService.changeStatus(command);
    }

    @Override
    public void unbindChannel(UnbindStoreChannelCommand command) {
        appService.unbindChannel(command);
    }
}
