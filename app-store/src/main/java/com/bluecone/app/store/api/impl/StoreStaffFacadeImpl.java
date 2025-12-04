package com.bluecone.app.store.api.impl;

import com.bluecone.app.store.api.StoreStaffFacade;
import com.bluecone.app.store.api.dto.StoreStaffView;
import com.bluecone.app.store.application.command.AddStoreStaffCommand;
import com.bluecone.app.store.application.command.BatchBindStoreStaffCommand;
import com.bluecone.app.store.application.command.ChangeStoreStaffRoleCommand;
import com.bluecone.app.store.application.command.RemoveStoreStaffCommand;
import com.bluecone.app.store.application.query.StoreStaffListQuery;
import com.bluecone.app.store.application.service.StoreStaffAppService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门店员工 Facade 实现。
 * <p>职责：转发请求至应用服务，保持接口与实现解耦。</p>
 */
@Service
public class StoreStaffFacadeImpl implements StoreStaffFacade {

    private final StoreStaffAppService appService;

    public StoreStaffFacadeImpl(StoreStaffAppService appService) {
        this.appService = appService;
    }

    @Override
    public List<StoreStaffView> list(StoreStaffListQuery query) {
        return appService.list(query);
    }

    @Override
    public void addStaff(AddStoreStaffCommand command) {
        appService.addStaff(command);
    }

    @Override
    public void removeStaff(RemoveStoreStaffCommand command) {
        appService.removeStaff(command);
    }

    @Override
    public void changeRole(ChangeStoreStaffRoleCommand command) {
        appService.changeRole(command);
    }

    @Override
    public void batchBindStaff(BatchBindStoreStaffCommand command) {
        appService.batchBindStaff(command);
    }
}
