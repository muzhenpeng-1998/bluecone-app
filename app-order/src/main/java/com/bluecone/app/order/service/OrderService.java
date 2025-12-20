package com.bluecone.app.order.service;

import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service("orderHelloService")
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public Map<String, Object> hello() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "hello, order");
        result.put("at", Instant.now().toString());
        return result;
    }

    /**
     * Find order by ID using current tenant context.
     * 
     * @param orderId the order ID
     * @return the order, or null if not found
     */
    public Order findById(Long orderId) {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new IllegalStateException("Tenant context is not set");
        }
        Long tenantId = Long.parseLong(tenantIdStr);
        return orderRepository.findById(tenantId, orderId);
    }
}
