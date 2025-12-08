package com.bluecone.app.infra.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluecone.app.infra.entity.OrderEntity;
import com.bluecone.app.infra.test.AbstractIntegrationTest;
import com.bluecone.app.infra.tenant.TenantContext;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrderMapperIT extends AbstractIntegrationTest {

    @Autowired
    private OrderMapper orderMapper;

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @Test
    void tenantInterceptorWritesAndFiltersRows() {
        TenantContext.setTenantId("1001");
        OrderEntity order = new OrderEntity();
        order.setAmount(new BigDecimal("19.99"));
        order.setStatus("CREATED");

        orderMapper.insert(order);

        TenantContext.setTenantId("1001");
        OrderEntity sameTenant = orderMapper.selectById(order.getId());
        assertThat(sameTenant).isNotNull();
        assertThat(sameTenant.getTenantId()).isEqualTo(1001L);

        TenantContext.setTenantId("2002");
        OrderEntity otherTenant = orderMapper.selectById(order.getId());
        assertThat(otherTenant).isNull();
    }
}
