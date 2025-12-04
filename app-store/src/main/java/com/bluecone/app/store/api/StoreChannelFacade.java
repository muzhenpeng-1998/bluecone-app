package com.bluecone.app.store.api;

import com.bluecone.app.store.api.dto.StoreChannelView;
import com.bluecone.app.store.application.command.BindStoreChannelCommand;
import com.bluecone.app.store.application.command.ChangeStoreChannelStatusCommand;
import com.bluecone.app.store.application.command.UnbindStoreChannelCommand;
import com.bluecone.app.store.application.command.UpdateStoreChannelConfigCommand;
import com.bluecone.app.store.application.query.StoreChannelListQuery;

import java.util.List;

/**
 * 门店渠道管理 Facade。
 * <p>职责：对外暴露门店与外部渠道绑定相关能力，屏蔽底层实现。</p>
 * <p>高隔离：其他模块仅依赖本接口 + DTO，不得直接访问 Mapper/ServiceImpl。</p>
 */
public interface StoreChannelFacade {

    /**
     * 列表查询门店渠道绑定信息。
     */
    List<StoreChannelView> list(StoreChannelListQuery query);

    /**
     * 根据主键获取单个渠道绑定详情。
     */
    StoreChannelView getById(Long tenantId, Long storeId, Long channelId);

    /**
     * 绑定新的外部渠道。
     */
    void bindChannel(BindStoreChannelCommand command);

    /**
     * 更新渠道配置 JSON。
     */
    void updateChannelConfig(UpdateStoreChannelConfigCommand command);

    /**
     * 修改渠道状态（启用/停用）。
     */
    void changeChannelStatus(ChangeStoreChannelStatusCommand command);

    /**
     * 解绑渠道（逻辑删除）。
     */
    void unbindChannel(UnbindStoreChannelCommand command);
}
