package com.bluecone.app.order.infra.facade;

import com.bluecone.app.order.domain.facade.InventoryFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 库存 Facade No-op 实现（M4 阶段）。
 * 
 * <h3>职责：</h3>
 * <ul>
 *   <li>M4 阶段的 No-op 实现，不实际操作库存</li>
 *   <li>记录日志，方便调试和追踪</li>
 *   <li>为后续真实实现预留接口</li>
 * </ul>
 * 
 * <h3>实现策略：</h3>
 * <ul>
 *   <li>所有方法只记录日志，不执行实际库存操作</li>
 *   <li>后续替换为真实的库存操作实现（通过事件/Outbox 异步调用）</li>
 * </ul>
 */
@Slf4j
@Component
public class NoOpInventoryFacade implements InventoryFacade {
    
    /**
     * 释放库存预占（订单取消时调用）。
     * <p>M4 阶段：No-op 实现，只记录日志，不实际操作库存。</p>
     * <p>TODO：后续实现真实的库存释放逻辑（通过事件/Outbox 异步调用）。</p>
     * 
     * @param tenantId 租户ID
     * @param storeId 门店ID
     * @param orderId 订单ID
     * @param reasonCode 释放原因码
     */
    @Override
    public void releaseReservation(Long tenantId, Long storeId, Long orderId, String reasonCode) {
        log.info("【No-op】释放库存预占：tenantId={}, storeId={}, orderId={}, reasonCode={}", 
                tenantId, storeId, orderId, reasonCode);
        log.warn("当前为 No-op 实现，未实际操作库存，后续需实现真实的库存释放逻辑");
        
        // M4 阶段：No-op 实现，不实际操作库存
        // TODO: 后续通过事件/Outbox 异步调用库存模块的释放预占接口
    }
    
    /**
     * 回补库存（订单退款时调用，已扣减的情况）。
     * <p>M4 阶段：No-op 实现，只记录日志，不实际操作库存。</p>
     * <p>TODO：后续实现真实的库存回补逻辑（通过事件/Outbox 异步调用）。</p>
     * 
     * @param tenantId 租户ID
     * @param storeId 门店ID
     * @param orderId 订单ID
     * @param reasonCode 回补原因码
     */
    @Override
    public void compensateInventory(Long tenantId, Long storeId, Long orderId, String reasonCode) {
        log.info("【No-op】回补库存：tenantId={}, storeId={}, orderId={}, reasonCode={}", 
                tenantId, storeId, orderId, reasonCode);
        log.warn("当前为 No-op 实现，未实际操作库存，后续需实现真实的库存回补逻辑");
        
        // M4 阶段：No-op 实现，不实际操作库存
        // TODO: 后续通过事件/Outbox 异步调用库存模块的回补接口
    }
    
    /**
     * 扣减库存（订单完成时调用，如果采用完成时扣减策略）。
     * <p>M4 阶段：No-op 实现，只记录日志，不实际操作库存。</p>
     * <p>TODO：后续根据库存策略实现真实的扣减逻辑（通过事件/Outbox 异步调用）。</p>
     * 
     * @param tenantId 租户ID
     * @param storeId 门店ID
     * @param orderId 订单ID
     */
    @Override
    public void deductInventory(Long tenantId, Long storeId, Long orderId) {
        log.info("【No-op】扣减库存：tenantId={}, storeId={}, orderId={}", tenantId, storeId, orderId);
        log.warn("当前为 No-op 实现，未实际操作库存，后续需根据库存策略实现真实的扣减逻辑");
        
        // M4 阶段：No-op 实现，不实际操作库存
        // TODO: 后续通过事件/Outbox 异步调用库存模块的扣减接口（如果采用完成时扣减策略）
    }
}
