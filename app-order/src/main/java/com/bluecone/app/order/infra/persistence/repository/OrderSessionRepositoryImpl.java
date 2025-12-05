package com.bluecone.app.order.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bluecone.app.order.domain.model.OrderSession;
import com.bluecone.app.order.domain.repository.OrderSessionRepository;
import com.bluecone.app.order.infra.persistence.converter.OrderConverter;
import com.bluecone.app.order.infra.persistence.mapper.OrderSessionMapper;
import com.bluecone.app.order.infra.persistence.po.OrderSessionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderSessionRepositoryImpl implements OrderSessionRepository {

    private final OrderSessionMapper orderSessionMapper;

    @Override
    public OrderSession findBySessionId(Long tenantId, String sessionId) {
        OrderSessionPO po = orderSessionMapper.selectOne(new LambdaQueryWrapper<OrderSessionPO>()
                .eq(OrderSessionPO::getTenantId, tenantId)
                .eq(OrderSessionPO::getSessionId, sessionId));
        return OrderConverter.toDomain(po);
    }

    @Override
    public void save(OrderSession session) {
        if (session == null) {
            return;
        }
        OrderSessionPO po = OrderConverter.toSessionPO(session);
        orderSessionMapper.insert(po);
    }

    @Override
    public boolean updateWithVersionCheck(OrderSession session) {
        if (session == null || session.getId() == null || session.getTenantId() == null) {
            return false;
        }
        Integer oldVersion = session.getVersion();
        OrderSessionPO po = OrderConverter.toSessionPO(session);
        po.setVersion(oldVersion == null ? 1 : oldVersion + 1);
        int updated = orderSessionMapper.update(po, new LambdaUpdateWrapper<OrderSessionPO>()
                .eq(OrderSessionPO::getTenantId, session.getTenantId())
                .eq(OrderSessionPO::getSessionId, session.getSessionId())
                .eq(oldVersion != null, OrderSessionPO::getVersion, oldVersion));
        if (updated > 0) {
            session.setVersion(po.getVersion());
            return true;
        }
        return false;
    }
}
