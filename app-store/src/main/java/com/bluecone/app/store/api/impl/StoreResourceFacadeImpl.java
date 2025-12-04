package com.bluecone.app.store.api.impl;

import com.bluecone.app.store.api.StoreResourceFacade;
import com.bluecone.app.store.api.dto.StoreResourceView;
import com.bluecone.app.store.application.command.ChangeStoreResourceStatusCommand;
import com.bluecone.app.store.application.command.CreateStoreResourceCommand;
import com.bluecone.app.store.application.command.UpdateStoreResourceCommand;
import com.bluecone.app.store.application.query.StoreResourceListQuery;
import com.bluecone.app.store.application.service.StoreResourceAppService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门店资源 Facade 实现。
 * <p>职责：承接调用并委派应用服务，保持接口与实现解耦。</p>
 */
@Service
public class StoreResourceFacadeImpl implements StoreResourceFacade {

    private final StoreResourceAppService appService;

    public StoreResourceFacadeImpl(StoreResourceAppService appService) {
        this.appService = appService;
    }

    @Override
    public List<StoreResourceView> list(StoreResourceListQuery query) {
        return appService.list(query);
    }

    @Override
    public StoreResourceView getById(Long tenantId, Long storeId, Long resourceId) {
        return appService.getById(tenantId, storeId, resourceId);
    }

    @Override
    public void createResource(CreateStoreResourceCommand command) {
        appService.create(command);
    }

    @Override
    public void updateResource(UpdateStoreResourceCommand command) {
        appService.update(command);
    }

    @Override
    public void changeStatus(ChangeStoreResourceStatusCommand command) {
        appService.changeStatus(command);
    }
}
