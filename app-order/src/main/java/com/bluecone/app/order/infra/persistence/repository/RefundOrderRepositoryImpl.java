package com.bluecone.app.order.infra.persistence.repository;

import com.bluecone.app.order.domain.model.RefundOrder;
import com.bluecone.app.order.domain.repository.RefundOrderRepository;
import com.bluecone.app.order.infra.persistence.converter.RefundOrderConverter;
import com.bluecone.app.order.infra.persistence.mapper.RefundOrderMapper;
import com.bluecone.app.order.infra.persistence.po.RefundOrderPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * 退款单仓储实现。
 * 
 * <h3>职责：</h3>
 * <ul>
 *   <li>退款单的持久化存储与查询</li>
 *   <li>领域模型与持久化对象之间的转换</li>
 *   <li>支持乐观锁更新</li>
 * </ul>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RefundOrderRepositoryImpl implements RefundOrderRepository {
    
    private final RefundOrderMapper refundOrderMapper;
    private final RefundOrderConverter refundOrderConverter;
    
    /**
     * 根据租户和退款单ID查询退款单，不存在返回 null。
     * 
     * @param tenantId 租户ID
     * @param refundOrderId 退款单ID
     * @return 退款单聚合根，不存在返回 null
     */
    @Override
    public RefundOrder findById(Long tenantId, Long refundOrderId) {
        log.debug("查询退款单：tenantId={}, refundOrderId={}", tenantId, refundOrderId);
        RefundOrderPO po = refundOrderMapper.findById(tenantId, refundOrderId);
        return refundOrderConverter.toDomain(po);
    }
    
    /**
     * 根据租户和幂等键查询退款单，用于幂等性保护。
     * 
     * @param tenantId 租户ID
     * @param idemKey 幂等键（格式：{tenantId}:{storeId}:{orderId}:refund:{requestId}）
     * @return 退款单聚合根，不存在返回 null
     */
    @Override
    public RefundOrder findByIdemKey(Long tenantId, String idemKey) {
        log.debug("根据幂等键查询退款单：tenantId={}, idemKey={}", tenantId, idemKey);
        RefundOrderPO po = refundOrderMapper.findByIdemKey(tenantId, idemKey);
        return refundOrderConverter.toDomain(po);
    }
    
    /**
     * 根据租户和订单ID查询最近一笔退款单（用于取消订单后查询退款状态）。
     * 
     * @param tenantId 租户ID
     * @param orderId 订单ID
     * @return 退款单聚合根，不存在返回 null
     */
    @Override
    public RefundOrder findLatestByOrderId(Long tenantId, Long orderId) {
        log.debug("查询订单最近一笔退款单：tenantId={}, orderId={}", tenantId, orderId);
        RefundOrderPO po = refundOrderMapper.findLatestByOrderId(tenantId, orderId);
        return refundOrderConverter.toDomain(po);
    }
    
    /**
     * 新建退款单。
     * 
     * @param refundOrder 退款单聚合根
     */
    @Override
    public void save(RefundOrder refundOrder) {
        log.debug("保存退款单：refundOrderId={}, orderId={}, idemKey={}", 
                refundOrder.getId(), refundOrder.getOrderId(), refundOrder.getIdemKey());
        RefundOrderPO po = refundOrderConverter.toPO(refundOrder);
        int rows = refundOrderMapper.insert(po);
        log.info("退款单保存成功：refundOrderId={}, orderId={}, rows={}", 
                refundOrder.getId(), refundOrder.getOrderId(), rows);
    }
    
    /**
     * 更新退款单（使用乐观锁）。
     * 
     * @param refundOrder 退款单聚合根
     * @return 更新行数（用于判断乐观锁是否成功）
     */
    @Override
    public int update(RefundOrder refundOrder) {
        log.debug("更新退款单：refundOrderId={}, orderId={}, version={}", 
                refundOrder.getId(), refundOrder.getOrderId(), refundOrder.getVersion());
        RefundOrderPO po = refundOrderConverter.toPO(refundOrder);
        int rows = refundOrderMapper.updateWithVersion(po);
        if (rows > 0) {
            log.info("退款单更新成功：refundOrderId={}, orderId={}, version={} -> {}", 
                    refundOrder.getId(), refundOrder.getOrderId(), 
                    refundOrder.getVersion(), refundOrder.getVersion() + 1);
        } else {
            log.warn("退款单更新失败（乐观锁冲突）：refundOrderId={}, orderId={}, version={}", 
                    refundOrder.getId(), refundOrder.getOrderId(), refundOrder.getVersion());
        }
        return rows;
    }
}
