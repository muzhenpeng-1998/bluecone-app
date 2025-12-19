package com.bluecone.app.order.application.impl;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.idempotency.api.IdempotencyRequest;
import com.bluecone.app.core.idempotency.api.IdempotencyTemplate;
import com.bluecone.app.core.idempotency.api.IdempotentResult;
import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.order.api.dto.OrderConfirmItemRequest;
import com.bluecone.app.order.api.dto.OrderSubmitRequest;
import com.bluecone.app.order.api.dto.OrderSubmitResponse;
import com.bluecone.app.order.application.OrderPreCheckService;
import com.bluecone.app.order.application.OrderSubmitApplicationService;
import com.bluecone.app.order.domain.enums.BizType;
import com.bluecone.app.order.domain.enums.OrderSource;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.domain.error.OrderErrorCode;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.model.OrderItem;
import com.bluecone.app.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单提交单应用服务实现（M0）。
 * <p>负责订单提交单的业务编排，包括幂等检查、关键校验、落库等。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSubmitApplicationServiceImpl implements OrderSubmitApplicationService {

    private final OrderRepository orderRepository;
    private final OrderPreCheckService orderPreCheckService;
    private final IdempotencyTemplate idempotencyTemplate;
    private final IdService idService;

    /**
     * 订单提交单（M0）。
     * <p>业务流程：</p>
     * <ol>
     *   <li>参数校验</li>
     *   <li>幂等检查（基于 tenantId + storeId + userId + clientRequestId）</li>
     *   <li>重做关键校验（至少：门店可接单 + 商品有效 + 价格版本一致）</li>
     *   <li>生成 publicOrderNo（对齐公共 ID 治理）</li>
     *   <li>落库订单与明细</li>
     *   <li>返回结果（WAIT_PAY）</li>
     * </ol>
     */
    @Override
    public OrderSubmitResponse submit(OrderSubmitRequest request) {
        // 1. 参数校验
        validateRequest(request);

        // 2. 幂等检查与执行
        // 幂等键规则：tenantId + storeId + userId + clientRequestId
        String idemKey = buildIdemKey(request);
        String requestHash = calculateRequestHash(request);

        IdempotencyRequest idemRequest = new IdempotencyRequest(
                request.getTenantId(),
                "ORDER_SUBMIT", // 业务类型
                idemKey,
                requestHash,
                Duration.ofHours(24), // 幂等记录有效期24小时
                Duration.ofSeconds(30), // 单次执行租约30秒
                false, // 不等待并发请求完成
                null
        );

        // 使用幂等模板执行业务逻辑
        IdempotentResult<OrderSubmitResponse> result = idempotencyTemplate.execute(
                idemRequest,
                OrderSubmitResponse.class,
                () -> doSubmit(request)
        );

        // 如果是重放结果，标记 idempotent=true
        if (result.replayed()) {
            OrderSubmitResponse response = result.value();
            response.setIdempotent(true);
            log.info("订单提交幂等返回：tenantId={}, storeId={}, userId={}, clientRequestId={}, orderId={}",
                    request.getTenantId(), request.getStoreId(), request.getUserId(),
                    request.getClientRequestId(), response.getOrderId());
            return response;
        }

        // 首次创建
        OrderSubmitResponse response = result.value();
        response.setIdempotent(false);
        log.info("订单提交成功：tenantId={}, storeId={}, userId={}, clientRequestId={}, orderId={}, publicOrderNo={}",
                request.getTenantId(), request.getStoreId(), request.getUserId(),
                request.getClientRequestId(), response.getOrderId(), response.getPublicOrderNo());
        return response;
    }

    /**
     * 实际的订单提交业务逻辑（在幂等保护下执行）。
     */
    @Transactional(rollbackFor = Exception.class)
    protected OrderSubmitResponse doSubmit(OrderSubmitRequest request) {
        // 3. 重做关键校验
        // 3.1 门店可接单校验
        orderPreCheckService.preCheck(
                request.getTenantId(),
                request.getStoreId(),
                request.getChannel(),
                LocalDateTime.now(),
                null // M0暂不传购物车摘要
        );

        // 3.2 商品校验（M0暂时跳过，预留接口位）
        // TODO: 调用 ProductFacade 校验商品是否存在、是否上架、库存是否充足等

        // 3.3 价格版本校验（M0暂时跳过，后续可校验 confirmToken 和 priceVersion）
        // TODO: 校验 confirmToken 是否有效、priceVersion 是否过期

        // 4. 生成订单ID和PublicOrderNo
        // 使用 IdScope.ORDER 生成 long 型订单ID
        Long orderId = idService.nextLong(IdScope.ORDER);
        String publicOrderNo = idService.nextPublicId(ResourceType.ORDER);

        // 5. 构建订单聚合根
        Order order = buildOrder(request, orderId, publicOrderNo);

        // 6. 落库订单与明细
        orderRepository.save(order);

        // 7. 构建响应
        return OrderSubmitResponse.builder()
                .orderId(order.getId())
                .publicOrderNo(order.getOrderNo())
                .status(order.getStatus().getCode())
                .payableAmount(order.getPayableAmount())
                .currency(order.getCurrency())
                .idempotent(false)
                .build();
    }

    /**
     * 校验请求参数。
     */
    private void validateRequest(OrderSubmitRequest request) {
        if (request == null) {
            throw new BusinessException(OrderErrorCode.ORDER_INVALID, "请求参数不能为空");
        }
        if (request.getTenantId() == null || request.getTenantId() <= 0) {
            throw new BusinessException(OrderErrorCode.ORDER_INVALID, "租户ID不能为空");
        }
        if (request.getStoreId() == null || request.getStoreId() <= 0) {
            throw new BusinessException(OrderErrorCode.ORDER_INVALID, "门店ID不能为空");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new BusinessException(OrderErrorCode.ORDER_INVALID, "用户ID不能为空");
        }
        if (request.getClientRequestId() == null || request.getClientRequestId().isBlank()) {
            throw new BusinessException(OrderErrorCode.ORDER_INVALID, "客户端请求ID不能为空");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(OrderErrorCode.ORDER_INVALID, "订单明细不能为空");
        }
        if (request.getDeliveryType() == null || request.getDeliveryType().isBlank()) {
            throw new BusinessException(OrderErrorCode.ORDER_INVALID, "配送类型不能为空");
        }

        // 校验明细项
        for (OrderConfirmItemRequest item : request.getItems()) {
            if (item.getSkuId() == null || item.getSkuId() <= 0) {
                throw new BusinessException(OrderErrorCode.ORDER_INVALID, "商品SKU ID不能为空");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException(OrderErrorCode.ORDER_INVALID, "商品数量必须大于0");
            }
        }
    }

    /**
     * 构建幂等键。
     * <p>规则：tenantId + storeId + userId + clientRequestId</p>
     */
    private String buildIdemKey(OrderSubmitRequest request) {
        return String.format("%d:%d:%d:%s",
                request.getTenantId(),
                request.getStoreId(),
                request.getUserId(),
                request.getClientRequestId());
    }

    /**
     * 计算请求摘要（SHA-256）。
     * <p>用于幂等冲突检测，确保同一个 idemKey 的请求内容一致。</p>
     */
    private String calculateRequestHash(OrderSubmitRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getTenantId()).append("|");
        sb.append(request.getStoreId()).append("|");
        sb.append(request.getUserId()).append("|");
        sb.append(request.getClientRequestId()).append("|");
        sb.append(request.getDeliveryType()).append("|");

        // 将明细项排序后拼接（保证幂等性）
        List<OrderConfirmItemRequest> sortedItems = request.getItems().stream()
                .sorted((a, b) -> Long.compare(a.getSkuId(), b.getSkuId()))
                .collect(Collectors.toList());
        for (OrderConfirmItemRequest item : sortedItems) {
            sb.append(item.getSkuId()).append(":").append(item.getQuantity()).append(",");
        }

        // 计算 SHA-256 摘要
        String raw = sb.toString();
        byte[] hash = org.apache.commons.codec.digest.DigestUtils.sha256(raw.getBytes(StandardCharsets.UTF_8));
        // 转换为十六进制字符串
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 构建订单聚合根。
     */
    private Order buildOrder(OrderSubmitRequest request, Long orderId, String publicOrderNo) {
        LocalDateTime now = LocalDateTime.now();

        // 构建订单明细
        List<OrderItem> items = request.getItems().stream()
                .map(item -> buildOrderItem(request, item, orderId, now))
                .collect(Collectors.toList());

        // 计算金额
        BigDecimal totalAmount = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountAmount = BigDecimal.ZERO; // M0暂不支持优惠
        BigDecimal payableAmount = totalAmount.subtract(discountAmount);

        // 构建订单聚合根
        return Order.builder()
                .id(orderId)
                .tenantId(request.getTenantId())
                .storeId(request.getStoreId())
                .userId(request.getUserId())
                .orderNo(publicOrderNo)
                .clientOrderNo(request.getClientRequestId())
                .bizType(BizType.fromCode(request.getDeliveryType()))
                .orderSource(OrderSource.fromCode(request.getOrderSource()))
                .channel(request.getChannel())
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .payableAmount(payableAmount)
                .currency("CNY")
                .status(OrderStatus.WAIT_PAY) // M0默认状态：待支付
                .remark(request.getRemark())
                .items(items)
                .version(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * 构建订单明细项。
     */
    private OrderItem buildOrderItem(OrderSubmitRequest request, OrderConfirmItemRequest itemReq, Long orderId, LocalDateTime now) {
        // M0暂时使用客户端传递的价格，后续应从商品服务获取实时价格
        BigDecimal unitPrice = itemReq.getClientUnitPrice() != null
                ? itemReq.getClientUnitPrice()
                : BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO; // M0暂不支持优惠
        BigDecimal payableAmount = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()))
                .subtract(discountAmount);

        // 生成明细ID（使用 IdScope.ORDER_ITEM）
        Long itemId = idService.nextLong(IdScope.ORDER_ITEM);

        return OrderItem.builder()
                .id(itemId)
                .orderId(orderId)
                .tenantId(request.getTenantId())
                .storeId(request.getStoreId())
                .productId(itemReq.getProductId())
                .skuId(itemReq.getSkuId())
                .productName("商品名称-" + itemReq.getSkuId()) // M0暂时使用占位符，后续从商品服务获取
                .skuName("SKU名称-" + itemReq.getSkuId()) // M0暂时使用占位符
                .productCode("CODE-" + itemReq.getSkuId()) // M0暂时使用占位符
                .quantity(itemReq.getQuantity())
                .unitPrice(unitPrice)
                .discountAmount(discountAmount)
                .payableAmount(payableAmount)
                .attrs(itemReq.getAttrs())
                .remark(itemReq.getRemark())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
