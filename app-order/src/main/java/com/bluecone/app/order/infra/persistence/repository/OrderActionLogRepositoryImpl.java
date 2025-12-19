package com.bluecone.app.order.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.order.domain.model.OrderActionLog;
import com.bluecone.app.order.domain.repository.OrderActionLogRepository;
import com.bluecone.app.order.infra.persistence.converter.OrderActionLogConverter;
import com.bluecone.app.order.infra.persistence.mapper.OrderActionLogMapper;
import com.bluecone.app.order.infra.persistence.po.OrderActionLogPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 订单动作幂等日志仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class OrderActionLogRepositoryImpl implements OrderActionLogRepository {

    private final OrderActionLogMapper mapper;

    @Override
    public OrderActionLog findByActionKey(Long tenantId, String actionKey) {
        if (tenantId == null || actionKey == null || actionKey.isBlank()) {
            return null;
        }
        OrderActionLogPO po = mapper.selectOne(new LambdaQueryWrapper<OrderActionLogPO>()
                .eq(OrderActionLogPO::getTenantId, tenantId)
                .eq(OrderActionLogPO::getActionKey, actionKey)
                .last("LIMIT 1"));
        return OrderActionLogConverter.toDomain(po);
    }

    @Override
    public void save(OrderActionLog log) {
        if (log == null) {
            return;
        }
        OrderActionLogPO po = OrderActionLogConverter.toPO(log);
        mapper.insert(po);
        log.setId(po.getId());
    }

    @Override
    public void update(OrderActionLog log) {
        if (log == null || log.getId() == null) {
            return;
        }
        OrderActionLogPO po = OrderActionLogConverter.toPO(log);
        mapper.updateById(po);
    }
}
