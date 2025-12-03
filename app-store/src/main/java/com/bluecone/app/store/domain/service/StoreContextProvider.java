package com.bluecone.app.store.domain.service;

import com.bluecone.app.store.api.dto.StoreBaseView;
import com.bluecone.app.store.api.dto.StoreOrderAcceptResult;
import com.bluecone.app.store.api.dto.StoreOrderSnapshot;

import java.time.LocalDateTime;

/**
 * 高性能的门店上下文访问接口，对外只暴露领域语义，隐藏底层表结构与 Mapper。
 * <p>高隔离：订单、库存、打印等上层只能通过此接口获取门店信息，避免直接依赖实体或 Mapper。</p>
 * <p>高稳定：后续可在实现层加入多级缓存与降级策略（Redis 异常回退 DB，DB 异常回退缓存快照）。</p>
 * <p>高并发：依赖 StoreConfig/StoreOrderSnapshot 快照，减少同一请求链路中的多次分表查询。</p>
 */
public interface StoreContextProvider {

    /**
     * 获取门店基础信息视图，用于列表或展示。
     *
     * @param tenantId 租户 ID
     * @param storeId  门店 ID
     * @return 基础信息视图
     */
    StoreBaseView getStoreBase(Long tenantId, Long storeId);

    /**
     * 获取下单场景的门店快照。
     *
     * @param tenantId    租户 ID
     * @param storeId     门店 ID
     * @param now         当前时间（用于营业校验）
     * @param channelType 渠道类型（小程序、三方外卖等）
     * @return 订单视角快照
     */
    StoreOrderSnapshot getOrderSnapshot(Long tenantId, Long storeId, LocalDateTime now, String channelType);

    /**
     * 校验是否可接单，集中封装状态、能力、营业时间、渠道绑定等规则。
     *
     * @param tenantId   租户 ID
     * @param storeId    门店 ID
     * @param capability 请求能力（外卖/自取等）
     * @param now        当前时间
     * @param channelType 渠道类型
     * @return 可接单结果，含失败原因
     */
    StoreOrderAcceptResult checkOrderAcceptable(Long tenantId, Long storeId, String capability, LocalDateTime now, String channelType);
}
