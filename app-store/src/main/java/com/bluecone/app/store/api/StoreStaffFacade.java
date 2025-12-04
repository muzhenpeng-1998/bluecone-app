package com.bluecone.app.store.api;

import com.bluecone.app.store.api.dto.StoreStaffView;
import com.bluecone.app.store.application.command.AddStoreStaffCommand;
import com.bluecone.app.store.application.command.BatchBindStoreStaffCommand;
import com.bluecone.app.store.application.command.ChangeStoreStaffRoleCommand;
import com.bluecone.app.store.application.command.RemoveStoreStaffCommand;
import com.bluecone.app.store.application.query.StoreStaffListQuery;

import java.util.List;

/**
 * 门店员工关系 Facade。
 * <p>职责：对外暴露员工增删改查相关能力，屏蔽底层实现细节。</p>
 */
public interface StoreStaffFacade {

    List<StoreStaffView> list(StoreStaffListQuery query);

    /**
     * 为门店新增一名员工。
     */
    void addStaff(AddStoreStaffCommand command);

    /**
     * 移除门店员工关系。
     */
    void removeStaff(RemoveStoreStaffCommand command);

    /**
     * 调整门店员工角色。
     */
    void changeRole(ChangeStoreStaffRoleCommand command);

    /**
     * 批量绑定员工（如从总部导入）。
     */
    void batchBindStaff(BatchBindStoreStaffCommand command);
}
