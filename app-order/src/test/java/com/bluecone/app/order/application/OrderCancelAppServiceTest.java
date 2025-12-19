package com.bluecone.app.order.application;

import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.application.command.CancelOrderCommand;
import com.bluecone.app.order.application.impl.OrderCancelAppServiceImpl;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.domain.enums.PayStatus;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.model.OrderActionLog;
import com.bluecone.app.order.domain.repository.OrderActionLogRepository;
import com.bluecone.app.order.domain.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 订单取消应用服务测试（M4）。
 */
class OrderCancelAppServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private OrderActionLogRepository orderActionLogRepository;
    
    @Mock
    private RefundAppService refundAppService;
    
    private OrderCancelAppService orderCancelAppService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderCancelAppService = new OrderCancelAppServiceImpl(
                orderRepository,
                orderActionLogRepository,
                refundAppService
        );
    }
    
    /**
     * 测试场景1：WAIT_PAY 状态订单取消（不退款）。
     */
    @Test
    void testCancelOrder_WaitPay_NoPay() {
        // 准备测试数据
        Long tenantId = 1L;
        Long storeId = 100L;
        Long userId = 1000L;
        Long orderId = 10000L;
        String requestId = "req_test_001";
        
        CancelOrderCommand command = CancelOrderCommand.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .userId(userId)
                .orderId(orderId)
                .requestId(requestId)
                .reasonCode("USER_CANCEL")
                .reasonDesc("不想要了")
                .build();
        
        Order order = Order.builder()
                .id(orderId)
                .tenantId(tenantId)
                .storeId(storeId)
                .userId(userId)
                .status(OrderStatus.WAIT_PAY)
                .payStatus(PayStatus.UNPAID)
                .payableAmount(BigDecimal.valueOf(100))
                .version(0)
                .build();
        
        // Mock 行为
        when(orderActionLogRepository.findByIdemKey(eq(tenantId), any())).thenReturn(null);
        when(orderRepository.findById(tenantId, orderId)).thenReturn(order);
        
        // 执行测试
        orderCancelAppService.cancelOrder(command);
        
        // 验证结果
        assertEquals(OrderStatus.CANCELED, order.getStatus());
        assertNotNull(order.getCanceledAt());
        assertEquals("USER_CANCEL", order.getCancelReasonCode());
        
        // 验证方法调用
        verify(orderRepository, times(1)).update(order);
        verify(orderActionLogRepository, times(1)).save(any(OrderActionLog.class));
        verify(refundAppService, never()).applyRefund(any()); // 未支付，不退款
    }
    
    /**
     * 测试场景2：WAIT_ACCEPT 状态已支付订单取消（自动退款）。
     */
    @Test
    void testCancelOrder_WaitAccept_Paid() {
        // 准备测试数据
        Long tenantId = 1L;
        Long storeId = 100L;
        Long userId = 1000L;
        Long orderId = 10000L;
        String requestId = "req_test_002";
        
        CancelOrderCommand command = CancelOrderCommand.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .userId(userId)
                .orderId(orderId)
                .requestId(requestId)
                .reasonCode("USER_CANCEL")
                .reasonDesc("不想要了")
                .build();
        
        Order order = Order.builder()
                .id(orderId)
                .tenantId(tenantId)
                .storeId(storeId)
                .userId(userId)
                .orderNo("ord_test_002")
                .status(OrderStatus.WAIT_ACCEPT)
                .payStatus(PayStatus.PAID)
                .payableAmount(BigDecimal.valueOf(100))
                .version(0)
                .build();
        
        // Mock 行为
        when(orderActionLogRepository.findByIdemKey(eq(tenantId), any())).thenReturn(null);
        when(orderRepository.findById(tenantId, orderId)).thenReturn(order);
        
        // 执行测试
        orderCancelAppService.cancelOrder(command);
        
        // 验证结果
        assertEquals(OrderStatus.CANCELED, order.getStatus());
        assertNotNull(order.getCanceledAt());
        
        // 验证方法调用
        verify(orderRepository, times(1)).update(order);
        verify(orderActionLogRepository, times(1)).save(any(OrderActionLog.class));
        verify(refundAppService, times(1)).applyRefund(any()); // 已支付，自动退款
    }
    
    /**
     * 测试场景3：幂等性 - 重复取消同一订单。
     */
    @Test
    void testCancelOrder_Idempotent() {
        // 准备测试数据
        Long tenantId = 1L;
        Long orderId = 10000L;
        String requestId = "req_test_003";
        
        CancelOrderCommand command = CancelOrderCommand.builder()
                .tenantId(tenantId)
                .storeId(100L)
                .userId(1000L)
                .orderId(orderId)
                .requestId(requestId)
                .reasonCode("USER_CANCEL")
                .build();
        
        OrderActionLog existingLog = OrderActionLog.builder()
                .id(1L)
                .tenantId(tenantId)
                .orderId(orderId)
                .actionType("CANCEL")
                .build();
        
        // Mock 行为：已存在 action_log
        when(orderActionLogRepository.findByIdemKey(eq(tenantId), any())).thenReturn(existingLog);
        
        // 执行测试
        orderCancelAppService.cancelOrder(command);
        
        // 验证：不应再次执行取消逻辑
        verify(orderRepository, never()).findById(any(), any());
        verify(orderRepository, never()).update(any());
    }
    
    /**
     * 测试场景4：状态约束 - IN_PROGRESS 状态不允许取消。
     */
    @Test
    void testCancelOrder_InvalidStatus() {
        // 准备测试数据
        CancelOrderCommand command = CancelOrderCommand.builder()
                .tenantId(1L)
                .storeId(100L)
                .userId(1000L)
                .orderId(10000L)
                .requestId("req_test_004")
                .reasonCode("USER_CANCEL")
                .build();
        
        Order order = Order.builder()
                .id(10000L)
                .tenantId(1L)
                .userId(1000L)
                .status(OrderStatus.IN_PROGRESS) // 制作中，不允许取消
                .version(0)
                .build();
        
        // Mock 行为
        when(orderActionLogRepository.findByIdemKey(any(), any())).thenReturn(null);
        when(orderRepository.findById(1L, 10000L)).thenReturn(order);
        
        // 执行测试并验证异常
        assertThrows(BizException.class, () -> {
            orderCancelAppService.cancelOrder(command);
        });
    }
}
