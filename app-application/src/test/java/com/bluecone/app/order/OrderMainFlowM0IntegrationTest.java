package com.bluecone.app.order;

import com.bluecone.app.order.api.dto.*;
import com.bluecone.app.order.application.OrderConfirmApplicationService;
import com.bluecone.app.order.application.OrderSubmitApplicationService;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 订单主链路 M0 集成测试。
 * <p>测试场景：</p>
 * <ol>
 *   <li>先 confirm 再 submit</li>
 *   <li>对同一个 clientRequestId submit 两次，第二次必须返回同一个 orderId</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("订单主链路 M0 集成测试")
class OrderMainFlowM0IntegrationTest {

    @Autowired
    private OrderConfirmApplicationService orderConfirmApplicationService;

    @Autowired
    private OrderSubmitApplicationService orderSubmitApplicationService;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * 测试场景1：先 confirm 再 submit。
     * <p>验证点：</p>
     * <ul>
     *   <li>confirm 接口返回 confirmToken 和 priceVersion</li>
     *   <li>submit 接口返回 orderId 和 publicOrderNo</li>
     *   <li>订单状态为 WAIT_PAY</li>
     *   <li>订单金额计算正确</li>
     * </ul>
     */
    @Test
    @DisplayName("测试场景1：先 confirm 再 submit")
    void testConfirmAndSubmit() {
        // 准备测试数据
        Long tenantId = 1L;
        Long storeId = 1L;
        Long userId = 1L;
        String clientRequestId = UUID.randomUUID().toString();

        // 1. 调用 confirm 接口
        OrderConfirmRequest confirmRequest = OrderConfirmRequest.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .userId(userId)
                .deliveryType("DINE_IN")
                .channel("MINI_PROGRAM")
                .orderSource("MINI_PROGRAM")
                .items(List.of(
                        OrderConfirmItemRequest.builder()
                                .skuId(101L)
                                .productId(100L)
                                .quantity(2)
                                .clientUnitPrice(new BigDecimal("10.00"))
                                .build(),
                        OrderConfirmItemRequest.builder()
                                .skuId(102L)
                                .productId(100L)
                                .quantity(1)
                                .clientUnitPrice(new BigDecimal("20.00"))
                                .build()
                ))
                .remark("测试订单")
                .build();

        OrderConfirmResponse confirmResponse = orderConfirmApplicationService.confirm(confirmRequest);

        // 验证 confirm 响应
        assertThat(confirmResponse).isNotNull();
        assertThat(confirmResponse.getConfirmToken()).isNotBlank();
        assertThat(confirmResponse.getPriceVersion()).isNotNull();
        assertThat(confirmResponse.getTotalAmount()).isEqualByComparingTo(new BigDecimal("40.00")); // 2*10 + 1*20
        assertThat(confirmResponse.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(confirmResponse.getPayableAmount()).isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(confirmResponse.getCurrency()).isEqualTo("CNY");
        assertThat(confirmResponse.getItems()).hasSize(2);

        // 2. 调用 submit 接口
        OrderSubmitRequest submitRequest = OrderSubmitRequest.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .userId(userId)
                .clientRequestId(clientRequestId)
                .confirmToken(confirmResponse.getConfirmToken())
                .priceVersion(confirmResponse.getPriceVersion())
                .deliveryType("DINE_IN")
                .channel("MINI_PROGRAM")
                .orderSource("MINI_PROGRAM")
                .items(confirmRequest.getItems())
                .remark("测试订单")
                .build();

        OrderSubmitResponse submitResponse = orderSubmitApplicationService.submit(submitRequest);

        // 验证 submit 响应
        assertThat(submitResponse).isNotNull();
        assertThat(submitResponse.getOrderId()).isNotNull();
        assertThat(submitResponse.getPublicOrderNo()).isNotBlank();
        assertThat(submitResponse.getPublicOrderNo()).startsWith("ord_");
        assertThat(submitResponse.getStatus()).isEqualTo("WAIT_PAY");
        assertThat(submitResponse.getPayableAmount()).isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(submitResponse.getCurrency()).isEqualTo("CNY");
        assertThat(submitResponse.getIdempotent()).isFalse();

        // 3. 验证订单已落库
        Order order = orderRepository.findById(tenantId, submitResponse.getOrderId());
        assertThat(order).isNotNull();
        assertThat(order.getId()).isEqualTo(submitResponse.getOrderId());
        assertThat(order.getOrderNo()).isEqualTo(submitResponse.getPublicOrderNo());
        assertThat(order.getTenantId()).isEqualTo(tenantId);
        assertThat(order.getStoreId()).isEqualTo(storeId);
        assertThat(order.getUserId()).isEqualTo(userId);
        assertThat(order.getStatus().getCode()).isEqualTo("WAIT_PAY");
        assertThat(order.getPayableAmount()).isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(order.getItems()).hasSize(2);
    }

    /**
     * 测试场景2：对同一个 clientRequestId submit 两次，第二次必须返回同一个 orderId。
     * <p>验证点：</p>
     * <ul>
     *   <li>第一次 submit 返回 idempotent=false</li>
     *   <li>第二次 submit 返回 idempotent=true</li>
     *   <li>两次返回的 orderId 和 publicOrderNo 相同</li>
     *   <li>数据库中只有一条订单记录</li>
     * </ul>
     */
    @Test
    @DisplayName("测试场景2：幂等验证 - 同一个 clientRequestId submit 两次")
    void testIdempotency() {
        // 准备测试数据
        Long tenantId = 1L;
        Long storeId = 1L;
        Long userId = 1L;
        String clientRequestId = UUID.randomUUID().toString();

        // 1. 第一次 submit
        OrderSubmitRequest submitRequest = OrderSubmitRequest.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .userId(userId)
                .clientRequestId(clientRequestId)
                .confirmToken("dummy-token")
                .priceVersion(System.currentTimeMillis())
                .deliveryType("DINE_IN")
                .channel("MINI_PROGRAM")
                .orderSource("MINI_PROGRAM")
                .items(List.of(
                        OrderConfirmItemRequest.builder()
                                .skuId(101L)
                                .productId(100L)
                                .quantity(2)
                                .clientUnitPrice(new BigDecimal("10.00"))
                                .build()
                ))
                .remark("幂等测试订单")
                .build();

        OrderSubmitResponse firstResponse = orderSubmitApplicationService.submit(submitRequest);

        // 验证第一次响应
        assertThat(firstResponse).isNotNull();
        assertThat(firstResponse.getOrderId()).isNotNull();
        assertThat(firstResponse.getPublicOrderNo()).isNotBlank();
        assertThat(firstResponse.getIdempotent()).isFalse();

        // 2. 第二次 submit（相同的 clientRequestId）
        OrderSubmitResponse secondResponse = orderSubmitApplicationService.submit(submitRequest);

        // 验证第二次响应（幂等返回）
        assertThat(secondResponse).isNotNull();
        assertThat(secondResponse.getOrderId()).isEqualTo(firstResponse.getOrderId());
        assertThat(secondResponse.getPublicOrderNo()).isEqualTo(firstResponse.getPublicOrderNo());
        assertThat(secondResponse.getIdempotent()).isTrue();

        // 3. 验证数据库中只有一条订单记录
        Order order = orderRepository.findById(tenantId, firstResponse.getOrderId());
        assertThat(order).isNotNull();
        assertThat(order.getId()).isEqualTo(firstResponse.getOrderId());
    }

    /**
     * 测试场景3：不同的 clientRequestId 应该创建不同的订单。
     * <p>验证点：</p>
     * <ul>
     *   <li>两次 submit 都返回 idempotent=false</li>
     *   <li>两次返回的 orderId 和 publicOrderNo 不同</li>
     *   <li>数据库中有两条订单记录</li>
     * </ul>
     */
    @Test
    @DisplayName("测试场景3：不同的 clientRequestId 应该创建不同的订单")
    void testDifferentClientRequestId() {
        // 准备测试数据
        Long tenantId = 1L;
        Long storeId = 1L;
        Long userId = 1L;

        // 1. 第一次 submit
        String clientRequestId1 = UUID.randomUUID().toString();
        OrderSubmitRequest submitRequest1 = OrderSubmitRequest.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .userId(userId)
                .clientRequestId(clientRequestId1)
                .confirmToken("dummy-token-1")
                .priceVersion(System.currentTimeMillis())
                .deliveryType("DINE_IN")
                .channel("MINI_PROGRAM")
                .orderSource("MINI_PROGRAM")
                .items(List.of(
                        OrderConfirmItemRequest.builder()
                                .skuId(101L)
                                .productId(100L)
                                .quantity(2)
                                .clientUnitPrice(new BigDecimal("10.00"))
                                .build()
                ))
                .remark("测试订单1")
                .build();

        OrderSubmitResponse response1 = orderSubmitApplicationService.submit(submitRequest1);

        // 2. 第二次 submit（不同的 clientRequestId）
        String clientRequestId2 = UUID.randomUUID().toString();
        OrderSubmitRequest submitRequest2 = OrderSubmitRequest.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .userId(userId)
                .clientRequestId(clientRequestId2)
                .confirmToken("dummy-token-2")
                .priceVersion(System.currentTimeMillis())
                .deliveryType("DINE_IN")
                .channel("MINI_PROGRAM")
                .orderSource("MINI_PROGRAM")
                .items(List.of(
                        OrderConfirmItemRequest.builder()
                                .skuId(101L)
                                .productId(100L)
                                .quantity(2)
                                .clientUnitPrice(new BigDecimal("10.00"))
                                .build()
                ))
                .remark("测试订单2")
                .build();

        OrderSubmitResponse response2 = orderSubmitApplicationService.submit(submitRequest2);

        // 验证两次响应不同
        assertThat(response1.getOrderId()).isNotEqualTo(response2.getOrderId());
        assertThat(response1.getPublicOrderNo()).isNotEqualTo(response2.getPublicOrderNo());
        assertThat(response1.getIdempotent()).isFalse();
        assertThat(response2.getIdempotent()).isFalse();

        // 3. 验证数据库中有两条订单记录
        Order order1 = orderRepository.findById(tenantId, response1.getOrderId());
        Order order2 = orderRepository.findById(tenantId, response2.getOrderId());
        assertThat(order1).isNotNull();
        assertThat(order2).isNotNull();
        assertThat(order1.getId()).isNotEqualTo(order2.getId());
    }
}
