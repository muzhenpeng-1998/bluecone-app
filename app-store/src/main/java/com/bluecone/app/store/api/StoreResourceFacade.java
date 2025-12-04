package com.bluecone.app.store.api;

import com.bluecone.app.store.api.dto.StoreResourceView;
import com.bluecone.app.store.application.command.ChangeStoreResourceStatusCommand;
import com.bluecone.app.store.application.command.CreateStoreResourceCommand;
import com.bluecone.app.store.application.command.UpdateStoreResourceCommand;
import com.bluecone.app.store.application.query.StoreResourceListQuery;

import java.util.List;

/**
 * 门店资源管理 Facade（餐桌/包间/场地等）。
 * <p>高隔离：仅暴露资源相关读写接口，隐藏底层数据访问。</p>
 */
public interface StoreResourceFacade {

    List<StoreResourceView> list(StoreResourceListQuery query);

    StoreResourceView getById(Long tenantId, Long storeId, Long resourceId);

    void createResource(CreateStoreResourceCommand command);

    void updateResource(UpdateStoreResourceCommand command);

    void changeStatus(ChangeStoreResourceStatusCommand command);
}
