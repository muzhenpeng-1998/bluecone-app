package com.bluecone.app.api.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.model.OrderItem;
import com.bluecone.app.order.domain.repository.OrderRepository;
import com.bluecone.app.order.infra.persistence.po.OrderPO;
import com.bluecone.app.order.infra.persistence.po.OrderItemPO;
import com.bluecone.app.order.infra.persistence.mapper.OrderMapper;
import com.bluecone.app.order.infra.persistence.mapper.OrderItemMapper;
import com.bluecone.app.ops.api.dto.forensics.OrderForensicsView;
import com.bluecone.app.ops.service.OrderForensicsQueryService;
import com.bluecone.app.security.admin.RequireAdminPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单管理后台接口
 * 
 * 提供订单的查询和管理功能：
 * - 分页查询订单列表（支持状态、时间筛选）
 * - 查看订单详情（含计价快照、支付信息）
 * - 查看订单诊断信息（forensics视图）
 * 
 * 权限要求：
 * - 查看：order:view
 * - 管理：order:manage
 */
@Tag(name = "Admin - Order", description = "平台后台订单管理接口")
@Slf4j
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {
    
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderRepository orderRepository;
    private final OrderForensicsQueryService forensicsQueryService;
    
    /**
     * 分页查询订单列表
     * 
     * @param tenantId 租户ID
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @param status 订单状态
     * @param storeId 门店ID（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 订单分页列表
     */
    @GetMapping
    @RequireAdminPermission("order:view")
    public Page<OrderListView> listOrders(@RequestHeader("X-Tenant-Id") Long tenantId,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) Long storeId,
                                          @RequestParam(required = false) LocalDateTime startTime,
                                          @RequestParam(required = false) LocalDateTime endTime) {
        log.info("查询订单列表: tenantId={}, page={}, size={}, status={}, storeId={}, startTime={}, endTime={}", 
                tenantId, page, size, status, storeId, startTime, endTime);
        
        LambdaQueryWrapper<OrderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderPO::getTenantId, tenantId);
        
        if (StringUtils.hasText(status)) {
            wrapper.eq(OrderPO::getStatus, status);
        }
        if (storeId != null) {
            wrapper.eq(OrderPO::getStoreId, storeId);
        }
        if (startTime != null) {
            wrapper.ge(OrderPO::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(OrderPO::getCreatedAt, endTime);
        }
        
        wrapper.orderByDesc(OrderPO::getCreatedAt);
        
        Page<OrderPO> orderPage = orderMapper.selectPage(new Page<>(page, size), wrapper);
        
        // 转换为视图对象
        Page<OrderListView> viewPage = new Page<>(page, size, orderPage.getTotal());
        List<OrderListView> views = orderPage.getRecords().stream()
                .map(this::toListView)
                .collect(Collectors.toList());
        viewPage.setRecords(views);
        
        return viewPage;
    }
    
    /**
     * 查询订单详情
     * 
     * @param tenantId 租户ID
     * @param id 订单ID
     * @return 订单详情（含明细、支付信息等）
     */
    @GetMapping("/{id}")
    @RequireAdminPermission("order:view")
    public OrderDetailView getOrder(@RequestHeader("X-Tenant-Id") Long tenantId,
                                   @PathVariable Long id) {
        log.info("查询订单详情: tenantId={}, orderId={}", tenantId, id);
        
        // 租户隔离校验
        OrderPO order = orderMapper.selectOne(new LambdaQueryWrapper<OrderPO>()
                .eq(OrderPO::getTenantId, tenantId)
                .eq(OrderPO::getId, id));
        
        if (order == null) {
            throw new IllegalArgumentException("订单不存在或无权访问");
        }
        
        // 查询订单明细
        List<OrderItemPO> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemPO>()
                .eq(OrderItemPO::getTenantId, tenantId)
                .eq(OrderItemPO::getOrderId, id));
        
        return toDetailView(order, items);
    }
    
    /**
     * 查询订单诊断信息（forensics视图）
     * 
     * 聚合订单全链路数据用于运维诊断，包含：
     * - 订单基本信息
     * - 计价快照
     * - 优惠券使用记录
     * - 钱包扣款记录
     * - Outbox事件
     * - 消费日志
     * - 自动诊断结论
     * 
     * @param tenantId 租户ID
     * @param id 订单ID
     * @return 订单诊断视图
     */
    @GetMapping("/{id}/forensics")
    @RequireAdminPermission("order:view")
    public OrderForensicsView getOrderForensics(@RequestHeader("X-Tenant-Id") Long tenantId,
                                               @PathVariable Long id) {
        log.info("查询订单诊断信息: tenantId={}, orderId={}", tenantId, id);
        
        // 租户隔离校验
        OrderPO order = orderMapper.selectOne(new LambdaQueryWrapper<OrderPO>()
                .eq(OrderPO::getTenantId, tenantId)
                .eq(OrderPO::getId, id));
        
        if (order == null) {
            throw new IllegalArgumentException("订单不存在或无权访问");
        }
        
        // 调用诊断查询服务生成forensics视图
        return forensicsQueryService.queryForensics(tenantId, id);
    }
    
    /**
     * 转换为列表视图
     */
    private OrderListView toListView(OrderPO order) {
        OrderListView view = new OrderListView();
        view.setId(order.getId());
        view.setOrderNo(order.getOrderNo());
        view.setStoreId(order.getStoreId());
        view.setUserId(order.getUserId());
        view.setBizType(order.getBizType());
        view.setOrderSource(order.getOrderSource());
        view.setTotalAmount(order.getTotalAmount());
        view.setDiscountAmount(order.getDiscountAmount());
        view.setPayableAmount(order.getPayableAmount());
        view.setStatus(order.getStatus());
        view.setPayStatus(order.getPayStatus());
        view.setCreatedAt(order.getCreatedAt());
        view.setUpdatedAt(order.getUpdatedAt());
        return view;
    }
    
    /**
     * 转换为详情视图
     */
    private OrderDetailView toDetailView(OrderPO order, List<OrderItemPO> items) {
        OrderDetailView view = new OrderDetailView();
        view.setId(order.getId());
        view.setOrderNo(order.getOrderNo());
        view.setClientOrderNo(order.getClientOrderNo());
        view.setStoreId(order.getStoreId());
        view.setUserId(order.getUserId());
        view.setBizType(order.getBizType());
        view.setOrderSource(order.getOrderSource());
        view.setChannel(order.getChannel());
        view.setTotalAmount(order.getTotalAmount());
        view.setDiscountAmount(order.getDiscountAmount());
        view.setPayableAmount(order.getPayableAmount());
        view.setCurrency(order.getCurrency());
        view.setStatus(order.getStatus());
        view.setPayStatus(order.getPayStatus());
        view.setOrderRemark(order.getOrderRemark());
        view.setExtJson(order.getExtJson());
        view.setAcceptOperatorId(order.getAcceptOperatorId());
        view.setAcceptedAt(order.getAcceptedAt());
        view.setCreatedAt(order.getCreatedAt());
        view.setUpdatedAt(order.getUpdatedAt());
        
        // 生成forensics链接
        view.setForensicsUrl("/api/admin/orders/" + order.getId() + "/forensics");
        
        // 转换订单明细
        List<OrderDetailView.OrderItemView> itemViews = items.stream()
                .map(item -> {
                    OrderDetailView.OrderItemView itemView = new OrderDetailView.OrderItemView();
                    itemView.setId(item.getId());
                    itemView.setProductId(item.getProductId());
                    itemView.setSkuId(item.getSkuId());
                    itemView.setProductName(item.getProductName());
                    itemView.setSkuName(item.getSkuName());
                    itemView.setProductCode(item.getProductCode());
                    itemView.setQuantity(item.getQuantity());
                    itemView.setUnitPrice(item.getUnitPrice());
                    itemView.setDiscountAmount(item.getDiscountAmount());
                    itemView.setPayableAmount(item.getPayableAmount());
                    itemView.setAttrsJson(item.getAttrsJson());
                    itemView.setRemark(item.getRemark());
                    return itemView;
                })
                .collect(Collectors.toList());
        view.setItems(itemViews);
        
        return view;
    }
    
    // ===== DTO类 =====
    
    @Data
    public static class OrderListView {
        private Long id;
        private String orderNo;
        private Long storeId;
        private Long userId;
        private String bizType;
        private String orderSource;
        private BigDecimal totalAmount;
        private BigDecimal discountAmount;
        private BigDecimal payableAmount;
        private String status;
        private String payStatus;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
    
    @Data
    public static class OrderDetailView {
        private Long id;
        private String orderNo;
        private String clientOrderNo;
        private Long storeId;
        private Long userId;
        private String bizType;
        private String orderSource;
        private String channel;
        private BigDecimal totalAmount;
        private BigDecimal discountAmount;
        private BigDecimal payableAmount;
        private String currency;
        private String status;
        private String payStatus;
        private String orderRemark;
        private String extJson;
        private Long acceptOperatorId;
        private LocalDateTime acceptedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String forensicsUrl; // forensics诊断链接
        private List<OrderItemView> items;
        
        @Data
        public static class OrderItemView {
            private Long id;
            private Long productId;
            private Long skuId;
            private String productName;
            private String skuName;
            private String productCode;
            private Integer quantity;
            private BigDecimal unitPrice;
            private BigDecimal discountAmount;
            private BigDecimal payableAmount;
            private String attrsJson;
            private String remark;
        }
    }
}
