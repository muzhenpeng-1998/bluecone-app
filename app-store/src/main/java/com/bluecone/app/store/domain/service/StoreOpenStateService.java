package com.bluecone.app.store.domain.service;

import com.bluecone.app.store.api.dto.StoreOrderAcceptResult;
import com.bluecone.app.store.domain.model.StoreConfig;
import com.bluecone.app.store.domain.model.runtime.StoreRuntime;

import java.time.LocalDateTime;

/**
 * 集中封装「门店是否营业、是否可接某类订单」的业务规则。
 * <p>高隔离：上层通过领域服务而非直接解析表数据。</p>
 * <p>高并发：直接消费 StoreConfig 快照，避免频繁拆表查询。</p>
 */
public interface StoreOpenStateService {

    /**
     * 校验是否可接单，建议遵循以下判断顺序（具体逻辑后续补充）：
     * <ol>
     *     <li>检查门店 status 是否 OPEN。</li>
     *     <li>检查 openForOrders 是否为 true。</li>
     *     <li>检查 capability 是否已启用。</li>
     *     <li>检查特殊日（bc_store_special_day）。</li>
     *     <li>检查常规营业时间（bc_store_opening_hours）。</li>
     *     <li>检查渠道绑定状态（bc_store_channel）。</li>
     * </ol>
     *
     * @param config      门店配置快照
     * @param capability  请求能力
     * @param now         当前时间
     * @param channelType 渠道类型
     * @return 是否可接单及原因
     */
    StoreOrderAcceptResult check(StoreConfig config, String capability, LocalDateTime now, String channelType);

    /**
     * 最小版：仅基于运行时快照判断是否可接单。
     *
     * @param runtime 门店运行时快照
     * @param now     当前时间
     * @return true 可接单；false 不可接单
     */
    boolean isStoreOpenForOrder(StoreRuntime runtime, LocalDateTime now);
}
