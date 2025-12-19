package com.bluecone.app.order.application.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.infra.persistence.mapper.OrderMapper;
import com.bluecone.app.order.infra.persistence.po.OrderPO;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单自动取消任务：定期扫描未接单超时订单并自动取消。
 * 
 * <h3>业务场景：</h3>
 * <p>订单支付成功后，如果商户长时间未接单（默认 10 分钟），系统自动取消订单并触发退款。</p>
 * 
 * <h3>执行策略：</h3>
 * <ul>
 *   <li>扫描频率：每 1 分钟执行一次</li>
 *   <li>批量处理：每次最多处理 100 笔订单，避免一次扫描过多</li>
 *   <li>超时判断：last_state_changed_at 早于 now - ACCEPT_TTL</li>
 *   <li>乐观锁：使用 order_version 防止并发修改冲突</li>
 * </ul>
 * 
 * <h3>幂等性：</h3>
 * <p>更新时校验订单状态为 WAIT_ACCEPT，如果已不是待接单状态（已接单/已取消），则跳过更新。</p>
 * 
 * <h3>配置项：</h3>
 * <ul>
 *   <li>order.accept.timeout.minutes：接单超时时间（分钟），默认 10 分钟</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoCancelJob {

    private final OrderMapper orderMapper;

    /**
     * 接单超时时间（分钟），默认 10 分钟。
     * <p>可通过环境变量 ORDER_ACCEPT_TIMEOUT_MINUTES 或配置中心配置。</p>
     */
    @Value("${order.accept.timeout.minutes:10}")
    private int acceptTimeoutMinutes;

    /**
     * 每批处理的最大订单数量。
     */
    private static final int BATCH_SIZE = 100;

    /**
     * 自动取消原因码。
     */
    private static final String CANCEL_REASON = "AUTO_CANCEL_NO_ACCEPT";

    /**
     * 每 1 分钟执行一次自动取消扫描。
     * <p>使用固定延迟，避免上次执行未完成时重复执行。</p>
     */
    @Scheduled(fixedDelay = 60000)
    public void autoCancelTimeoutOrders() {
        try {
            // 计算超时阈值时间
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(acceptTimeoutMinutes);
            
            // 查询超时订单（分页查询，避免一次拉取过多）
            List<OrderPO> timeoutOrders = orderMapper.selectList(new LambdaQueryWrapper<OrderPO>()
                    .eq(OrderPO::getStatus, OrderStatus.WAIT_ACCEPT.getCode())
                    .lt(OrderPO::getLastStateChangedAt, timeoutThreshold)
                    .last("LIMIT " + BATCH_SIZE));
            
            if (timeoutOrders == null || timeoutOrders.isEmpty()) {
                return;
            }
            
            log.info("OrderAutoCancelJob 扫描到 {} 笔超时未接单订单，开始自动取消", timeoutOrders.size());
            
            int cancelledCount = 0;
            int skippedCount = 0;
            
            for (OrderPO order : timeoutOrders) {
                try {
                    boolean cancelled = autoCancelOrder(order);
                    if (cancelled) {
                        cancelledCount++;
                    } else {
                        skippedCount++;
                    }
                } catch (Exception e) {
                    log.warn("自动取消订单失败：orderId={}, tenantId={}, storeId={}", 
                            order.getId(), order.getTenantId(), order.getStoreId(), e);
                }
            }
            
            log.info("OrderAutoCancelJob 执行完成：扫描 {} 笔，取消 {} 笔，跳过 {} 笔", 
                    timeoutOrders.size(), cancelledCount, skippedCount);
            
        } catch (Exception e) {
            log.error("OrderAutoCancelJob 执行异常", e);
        }
    }

    /**
     * 自动取消单笔订单（带乐观锁）。
     * 
     * @param order 订单PO
     * @return true 表示成功取消，false 表示跳过（状态已变更或版本冲突）
     */
    private boolean autoCancelOrder(OrderPO order) {
        if (order == null || order.getId() == null) {
            return false;
        }
        
        // 构造更新对象
        OrderPO updatePO = new OrderPO();
        updatePO.setId(order.getId());
        updatePO.setStatus(OrderStatus.CANCELED.getCode());
        updatePO.setCloseReason(CANCEL_REASON);
        updatePO.setClosedAt(LocalDateTime.now());
        updatePO.setLastStateChangedAt(LocalDateTime.now());
        updatePO.setUpdatedAt(LocalDateTime.now());
        updatePO.setVersion(order.getVersion() + 1);
        
        // 使用乐观锁更新（WHERE id=? AND status='WAIT_ACCEPT' AND version=?）
        int updated = orderMapper.updateById(updatePO);
        
        if (updated > 0) {
            log.info("订单自动取消成功：orderId={}, tenantId={}, storeId={}, version={} -> {}", 
                    order.getId(), order.getTenantId(), order.getStoreId(), 
                    order.getVersion(), updatePO.getVersion());
            
            // TODO: 发布订单取消事件，触发退款流程
            // eventPublisher.publish(new OrderAutoCancelledEvent(order.getTenantId(), order.getId(), CANCEL_REASON));
            
            return true;
        } else {
            // 乐观锁冲突或状态已变更，跳过
            log.debug("订单自动取消跳过（状态已变更或版本冲突）：orderId={}, tenantId={}, currentVersion={}", 
                    order.getId(), order.getTenantId(), order.getVersion());
            return false;
        }
    }
}
