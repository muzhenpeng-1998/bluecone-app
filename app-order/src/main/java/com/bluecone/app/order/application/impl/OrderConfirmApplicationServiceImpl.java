package com.bluecone.app.order.application.impl;

import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.api.dto.*;
import com.bluecone.app.order.application.OrderConfirmApplicationService;
import com.bluecone.app.order.application.OrderPreCheckService;
import com.bluecone.app.order.domain.error.OrderErrorCode;
import com.bluecone.app.store.api.StoreFacade;
import com.bluecone.app.store.api.dto.StoreOrderAcceptResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单确认单应用服务实现（M0）。
 * <p>负责订单确认单的业务编排，包括门店校验、商品校验、价格计算等。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderConfirmApplicationServiceImpl implements OrderConfirmApplicationService {

    private final StoreFacade storeFacade;
    private final OrderPreCheckService orderPreCheckService;

    /**
     * 订单确认单（M0）。
     * <p>业务流程：</p>
     * <ol>
     *   <li>参数校验</li>
     *   <li>调用门店 precheck（复用已完成能力）</li>
     *   <li>调用商品校验（M0暂时跳过，预留接口位）</li>
     *   <li>计算价格（M0不做优惠，直接累加单价*数量）</li>
     *   <li>生成 confirmToken 和 priceVersion</li>
     *   <li>返回确认单响应</li>
     * </ol>
     */
    @Override
    public OrderConfirmResponse confirm(OrderConfirmRequest request) {
        // 1. 参数校验
        validateRequest(request);

        List<String> failureReasons = new ArrayList<>();

        // 2. 门店前置校验（调用已完成的 OrderPreCheckService）
        StoreOrderAcceptResult storeResult = checkStoreAcceptable(request, failureReasons);

        // 3. 商品校验（M0暂时跳过，预留接口位）
        // TODO: 调用 ProductFacade 校验商品是否存在、是否上架、库存是否充足等
        // 示例：List<SkuSnapshot> skuSnapshots = productFacade.getSkuSnapshots(request.getTenantId(), skuIds);

        // 4. 计算价格（M0不做优惠，直接累加单价*数量）
        List<OrderConfirmItemResponse> itemResponses = buildItemResponses(request);
        BigDecimal totalAmount = itemResponses.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountAmount = BigDecimal.ZERO; // M0暂不支持优惠
        BigDecimal payableAmount = totalAmount.subtract(discountAmount);

        // 5. 生成 confirmToken 和 priceVersion
        long priceVersion = System.currentTimeMillis();
        String confirmToken = generateConfirmToken(request, priceVersion, payableAmount);

        // 6. 构建响应
        return OrderConfirmResponse.builder()
                .confirmToken(confirmToken)
                .priceVersion(priceVersion)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .payableAmount(payableAmount)
                .currency("CNY")
                .items(itemResponses)
                .storeAcceptable(storeResult.isAcceptable())
                .storeRejectReasonCode(storeResult.getReasonCode())
                .storeRejectReasonMessage(storeResult.getReasonMessage())
                .failureReasons(failureReasons.isEmpty() ? null : failureReasons)
                .build();
    }

    /**
     * 校验请求参数。
     */
    private void validateRequest(OrderConfirmRequest request) {
        if (request == null) {
            throw new BizException(OrderErrorCode.ORDER_INVALID, "请求参数不能为空");
        }
        if (request.getTenantId() == null || request.getTenantId() <= 0) {
            throw new BizException(OrderErrorCode.ORDER_INVALID, "租户ID不能为空");
        }
        if (request.getStoreId() == null || request.getStoreId() <= 0) {
            throw new BizException(OrderErrorCode.ORDER_INVALID, "门店ID不能为空");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new BizException(OrderErrorCode.ORDER_INVALID, "用户ID不能为空");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException(OrderErrorCode.ORDER_INVALID, "订单明细不能为空");
        }
        if (request.getDeliveryType() == null || request.getDeliveryType().isBlank()) {
            throw new BizException(OrderErrorCode.ORDER_INVALID, "配送类型不能为空");
        }

        // 校验明细项
        for (OrderConfirmItemRequest item : request.getItems()) {
            if (item.getSkuId() == null || item.getSkuId() <= 0) {
                throw new BizException(OrderErrorCode.ORDER_INVALID, "商品SKU ID不能为空");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BizException(OrderErrorCode.ORDER_INVALID, "商品数量必须大于0");
            }
        }
    }

    /**
     * 检查门店是否可接单。
     * <p>调用已完成的 OrderPreCheckService，如果不可接单则记录失败原因但不抛异常（允许用户看到原因）。</p>
     */
    private StoreOrderAcceptResult checkStoreAcceptable(OrderConfirmRequest request, List<String> failureReasons) {
        try {
            // 调用门店侧的可接单校验
            StoreOrderAcceptResult result = storeFacade.checkOrderAcceptable(
                    request.getTenantId(),
                    request.getStoreId(),
                    null, // capability 参数暂时不传
                    LocalDateTime.now(),
                    request.getChannel()
            );

            if (!result.isAcceptable()) {
                String reason = String.format("门店当前不可接单：%s（原因码：%s）",
                        result.getReasonMessage(), result.getReasonCode());
                failureReasons.add(reason);
                log.warn("门店前置校验失败：tenantId={}, storeId={}, reasonCode={}, detail={}",
                        request.getTenantId(), request.getStoreId(), result.getReasonCode(), result.getDetail());
            }

            return result;
        } catch (Exception e) {
            log.error("门店前置校验异常：tenantId={}, storeId={}",
                    request.getTenantId(), request.getStoreId(), e);
            failureReasons.add("门店校验异常：" + e.getMessage());
            return StoreOrderAcceptResult.builder()
                    .acceptable(false)
                    .reasonCode("STORE_CHECK_ERROR")
                    .reasonMessage("门店校验异常")
                    .build();
        }
    }

    /**
     * 构建订单明细响应列表。
     * <p>M0暂时使用客户端传递的价格，后续应从商品服务获取实时价格。</p>
     */
    private List<OrderConfirmItemResponse> buildItemResponses(OrderConfirmRequest request) {
        return request.getItems().stream()
                .map(item -> {
                    // M0暂时使用客户端传递的价格，后续应从商品服务获取实时价格
                    BigDecimal unitPrice = item.getClientUnitPrice() != null
                            ? item.getClientUnitPrice()
                            : BigDecimal.ZERO;
                    BigDecimal discountAmount = BigDecimal.ZERO; // M0暂不支持优惠
                    BigDecimal payableAmount = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()))
                            .subtract(discountAmount);

                    return OrderConfirmItemResponse.builder()
                            .skuId(item.getSkuId())
                            .productId(item.getProductId())
                            .productName("商品名称-" + item.getSkuId()) // M0暂时使用占位符，后续从商品服务获取
                            .skuName("SKU名称-" + item.getSkuId()) // M0暂时使用占位符
                            .productCode("CODE-" + item.getSkuId()) // M0暂时使用占位符
                            .quantity(item.getQuantity())
                            .unitPrice(unitPrice)
                            .discountAmount(discountAmount)
                            .payableAmount(payableAmount)
                            .attrs(item.getAttrs())
                            .remark(item.getRemark())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 生成确认令牌（confirmToken）。
     * <p>规则：SHA-256(tenantId + storeId + userId + items + priceVersion + payableAmount)</p>
     * <p>用于后续提交单校验，防止价格篡改。</p>
     */
    private String generateConfirmToken(OrderConfirmRequest request, long priceVersion, BigDecimal payableAmount) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getTenantId()).append("|");
        sb.append(request.getStoreId()).append("|");
        sb.append(request.getUserId()).append("|");
        sb.append(priceVersion).append("|");
        sb.append(payableAmount.toPlainString()).append("|");

        // 将明细项排序后拼接（保证幂等性）
        List<OrderConfirmItemRequest> sortedItems = request.getItems().stream()
                .sorted((a, b) -> Long.compare(a.getSkuId(), b.getSkuId()))
                .collect(Collectors.toList());
        for (OrderConfirmItemRequest item : sortedItems) {
            sb.append(item.getSkuId()).append(":").append(item.getQuantity()).append(",");
        }

        // 计算 SHA-256 摘要
        String raw = sb.toString();
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }
}
