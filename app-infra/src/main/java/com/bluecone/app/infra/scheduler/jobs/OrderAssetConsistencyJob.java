package com.bluecone.app.infra.scheduler.jobs;

import com.bluecone.app.core.event.outbox.AggregateType;
import com.bluecone.app.core.event.outbox.EventType;
import com.bluecone.app.core.event.outbox.OutboxEvent;
import com.bluecone.app.infra.event.outbox.OutboxEventService;
import com.bluecone.app.infra.observability.metrics.JobMetrics;
import com.bluecone.app.infra.scheduler.annotation.BlueconeJob;
import com.bluecone.app.infra.scheduler.core.JobContext;
import com.bluecone.app.infra.scheduler.core.JobHandler;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单资产一致性补偿任务
 * 扫描最近 N 小时已支付订单，检查是否缺失资产提交痕迹，补发 Outbox 事件
 * 
 * 补偿策略：
 * 1. 扫描最近 N 小时（默认 24 小时）状态为 PAID 的订单
 * 2. 检查是否存在对应的 order.paid 事件
 * 3. 如果缺失，补发 order.paid 事件到 Outbox
 * 4. 由 Outbox Publisher 投递事件，触发资产提交
 * 
 * 注意：
 * - 该任务是兜底机制，正常情况下订单支付成功时会自动写入 Outbox
 * - 补发的事件会被消费者幂等处理，不会重复扣减资产
 * - 只补发事件，不直接修改资产，保持事件驱动的一致性
 */
@Slf4j
@Component
@BlueconeJob(
        code = "order_asset_consistency",
        name = "Order Asset Consistency Check",
        cron = "0 0 */2 * * ?",  // 每 2 小时执行一次
        timeoutSeconds = 300
)
@RequiredArgsConstructor
public class OrderAssetConsistencyJob implements JobHandler {
    
    private static final String JOB_NAME = "order_asset_consistency";
    
    private final JdbcTemplate jdbcTemplate;
    private final OutboxEventService outboxEventService;
    private final JobMetrics jobMetrics;
    
    /**
     * 扫描时间窗口（小时）
     * 可通过配置文件覆盖，默认 24 小时
     */
    @Value("${bluecone.consistency.scan-window-hours:24}")
    private int scanWindowHours;
    
    @Override
    public void handle(JobContext context) {
        String traceId = context.getTraceId();
        log.info("[OrderAssetConsistency] Starting consistency check, traceId={}, scanWindowHours={}", 
                traceId, scanWindowHours);
        
        Timer.Sample sample = jobMetrics.startExecutionTimer();
        LocalDateTime scanStartTime = LocalDateTime.now().minusHours(scanWindowHours);
        
        try {
            // 1. 扫描最近 N 小时已支付的订单
            List<Map<String, Object>> paidOrders = findRecentPaidOrders(scanStartTime);
            
            if (paidOrders.isEmpty()) {
                log.info("[OrderAssetConsistency] No paid orders found in recent {} hours, traceId={}", 
                        scanWindowHours, traceId);
                jobMetrics.recordExecutionSuccess(JOB_NAME);
                return;
            }
            
            log.info("[OrderAssetConsistency] Found {} paid orders, checking consistency, traceId={}", 
                    paidOrders.size(), traceId);
            
            // 2. 检查每个订单是否有对应的 order.paid 事件
            int missingCount = 0;
            int repairedCount = 0;
            
            for (Map<String, Object> order : paidOrders) {
                Long orderId = ((Number) order.get("id")).longValue();
                Long tenantId = getLongValue(order.get("tenant_id"));
                Long storeId = getLongValue(order.get("store_id"));
                Long userId = getLongValue(order.get("user_id"));
                Long couponId = getLongValue(order.get("coupon_id"));
                
                // 检查是否存在 order.paid 事件
                boolean hasEvent = hasOutboxEvent(orderId, EventType.ORDER_PAID.getCode());
                
                if (!hasEvent) {
                    missingCount++;
                    log.warn("[OrderAssetConsistency] Missing order.paid event: orderId={}, traceId={}", 
                            orderId, traceId);
                    
                    // 补发 order.paid 事件
                    try {
                        repairOrderPaidEvent(orderId, tenantId, storeId, userId, couponId);
                        repairedCount++;
                        log.info("[OrderAssetConsistency] Repaired order.paid event: orderId={}, traceId={}", 
                                orderId, traceId);
                    } catch (Exception e) {
                        log.error("[OrderAssetConsistency] Failed to repair order.paid event: orderId={}, traceId={}", 
                                orderId, traceId, e);
                    }
                }
            }
            
            log.info("[OrderAssetConsistency] Consistency check completed: " +
                    "total={}, missing={}, repaired={}, traceId={}", 
                    paidOrders.size(), missingCount, repairedCount, traceId);
            
            // Record metrics
            jobMetrics.recordConsistencyCheck(JOB_NAME, paidOrders.size(), missingCount, repairedCount);
            jobMetrics.recordExecutionSuccess(JOB_NAME);
            
        } catch (Exception e) {
            log.error("[OrderAssetConsistency] Consistency check failed: traceId={}", traceId, e);
            jobMetrics.recordExecutionFailure(JOB_NAME);
            throw e;
        } finally {
            jobMetrics.stopExecutionTimer(sample, JOB_NAME);
        }
    }
    
    /**
     * 查询最近 N 小时已支付的订单
     * 
     * @param scanStartTime 扫描开始时间
     * @return 订单列表
     */
    private List<Map<String, Object>> findRecentPaidOrders(LocalDateTime scanStartTime) {
        String sql = "SELECT id, tenant_id, store_id, user_id, coupon_id, " +
                "       total_amount, discount_amount, payable_amount " +
                "FROM bc_order " +
                "WHERE status = 'PAID' " +
                "AND pay_status = 'PAID' " +
                "AND updated_at >= ? " +
                "AND deleted = 0 " +
                "ORDER BY updated_at DESC " +
                "LIMIT 1000";  // 限制数量，避免一次处理过多
        
        return jdbcTemplate.queryForList(sql, scanStartTime);
    }
    
    /**
     * 检查是否存在 Outbox 事件
     * 
     * @param orderId 订单ID
     * @param eventType 事件类型
     * @return true=存在，false=不存在
     */
    private boolean hasOutboxEvent(Long orderId, String eventType) {
        String sql = "SELECT COUNT(*) FROM bc_outbox_event " +
                "WHERE aggregate_type = ? " +
                "AND aggregate_id = ? " +
                "AND event_type = ?";
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, 
                AggregateType.ORDER.getCode(), String.valueOf(orderId), eventType);
        
        return count != null && count > 0;
    }
    
    /**
     * 补发 order.paid 事件
     * 
     * @param orderId 订单ID
     * @param tenantId 租户ID
     * @param storeId 门店ID
     * @param userId 用户ID
     * @param couponId 优惠券ID
     */
    private void repairOrderPaidEvent(Long orderId, Long tenantId, Long storeId, 
                                      Long userId, Long couponId) {
        // 构建事件载荷
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("couponId", couponId);
        // 注意：这里简化处理，实际应该从订单中读取完整信息
        // 如 walletAmount、pointsUsed、pointsEarned 等
        
        // 构建事件元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", userId);
        metadata.put("source", "consistency_repair");
        metadata.put("repairedAt", LocalDateTime.now().toString());
        
        // 创建 Outbox 事件
        OutboxEvent event = OutboxEvent.forOrder(
                tenantId, 
                storeId, 
                orderId, 
                EventType.ORDER_PAID, 
                payload, 
                metadata
        );
        
        // 写入 Outbox
        outboxEventService.save(event);
        
        log.info("[OrderAssetConsistency] Order.paid event repaired: orderId={}, eventId={}", 
                orderId, event.getEventId());
    }
    
    /**
     * 安全获取 Long 值
     */
    private Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
}
