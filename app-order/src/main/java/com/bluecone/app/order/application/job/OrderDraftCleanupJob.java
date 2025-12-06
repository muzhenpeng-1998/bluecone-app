package com.bluecone.app.order.application.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.infra.cache.OrderDraftCacheService;
import com.bluecone.app.order.infra.persistence.mapper.OrderItemMapper;
import com.bluecone.app.order.infra.persistence.mapper.OrderMapper;
import com.bluecone.app.order.infra.persistence.po.OrderItemPO;
import com.bluecone.app.order.infra.persistence.po.OrderPO;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 草稿订单清理任务：定期清理过期草稿及缓存，防止堆积。
 */
@Component
public class OrderDraftCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(OrderDraftCleanupJob.class);
    private static final int BATCH_SIZE = 200;
    // 草稿过期阈值：24 小时未更新则视为过期。
    private static final LocalDateTime EXPIRE_THRESHOLD = LocalDateTime.now().minusHours(24);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderDraftCacheService orderDraftCacheService;

    public OrderDraftCleanupJob(OrderMapper orderMapper,
                                OrderItemMapper orderItemMapper,
                                OrderDraftCacheService orderDraftCacheService) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderDraftCacheService = orderDraftCacheService;
    }

    /**
     * 每 30 分钟清理一次。
     */
    @Scheduled(cron = "0 */30 * * * ?")
    public void cleanupExpiredDrafts() {
        int cleaned = 0;
        while (true) {
            List<OrderPO> expired = orderMapper.selectList(new LambdaQueryWrapper<OrderPO>()
                    .eq(OrderPO::getStatus, OrderStatus.DRAFT.getCode())
                    .lt(OrderPO::getUpdatedAt, LocalDateTime.now().minusHours(24))
                    .last("LIMIT " + BATCH_SIZE));
            if (expired == null || expired.isEmpty()) {
                break;
            }
            for (OrderPO po : expired) {
                try {
                    orderItemMapper.delete(new LambdaQueryWrapper<OrderItemPO>()
                            .eq(OrderItemPO::getTenantId, po.getTenantId())
                            .eq(OrderItemPO::getStoreId, po.getStoreId())
                            .eq(OrderItemPO::getOrderId, po.getId()));
                    orderMapper.delete(new LambdaQueryWrapper<OrderPO>()
                            .eq(OrderPO::getId, po.getId())
                            .eq(OrderPO::getTenantId, po.getTenantId())
                            .eq(OrderPO::getStoreId, po.getStoreId()));
                    orderDraftCacheService.evictCache(po.getTenantId(), po.getStoreId(), po.getUserId(), po.getChannel(), po.getOrderSource());
                    cleaned++;
                } catch (Exception ex) {
                    log.warn("cleanup draft failed, draftId={}, tenantId={}, storeId={}, userId={}", po.getId(), po.getTenantId(), po.getStoreId(), po.getUserId(), ex);
                }
            }
            if (expired.size() < BATCH_SIZE) {
                break;
            }
        }
        if (cleaned > 0) {
            log.info("OrderDraftCleanupJob cleaned {} expired drafts", cleaned);
        }
    }
}
