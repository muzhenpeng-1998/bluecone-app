package com.bluecone.app.order.application.impl;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.order.api.dto.*;
import com.bluecone.app.order.application.OrderConfirmApplicationService;
import com.bluecone.app.order.application.OrderPreCheckService;
import com.bluecone.app.order.domain.error.OrderErrorCode;
import com.bluecone.app.pricing.api.dto.PricingItem;
import com.bluecone.app.pricing.api.dto.PricingQuote;
import com.bluecone.app.pricing.api.dto.PricingRequest;
import com.bluecone.app.pricing.api.facade.PricingFacade;
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
    private final PricingFacade pricingFacade;

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

        // 4. 调用统一计价引擎计算价格
        PricingQuote quote = calculatePrice(request);
        
        // 构建订单明细响应
        List<OrderConfirmItemResponse> itemResponses = buildItemResponsesFromQuote(request, quote);

        // 5. 生成 confirmToken 和 priceVersion
        String priceVersion = quote.getQuoteId(); // 使用 quoteId 作为 priceVersion
        String confirmToken = generateConfirmToken(request, priceVersion, quote.getPayableAmount());

        // 6. 构建响应
        return OrderConfirmResponse.builder()
                .confirmToken(confirmToken)
                .priceVersion(Long.parseLong(priceVersion.substring(0, Math.min(13, priceVersion.length())), 16)) // 兼容旧接口
                .totalAmount(quote.getOriginalAmount())
                .discountAmount(calculateTotalDiscount(quote))
                .payableAmount(quote.getPayableAmount())
                .currency(quote.getCurrency())
                .items(itemResponses)
                .storeAcceptable(storeResult.isAcceptable())
                .storeRejectReasonCode(storeResult.getReasonCode())
                .storeRejectReasonMessage(storeResult.getReasonMessage())
                .failureReasons(failureReasons.isEmpty() ? null : failureReasons)
                .build();
    }
    
    /**
     * 调用统一计价引擎计算价格
     */
    private PricingQuote calculatePrice(OrderConfirmRequest request) {
        try {
            // 构建计价请求
            PricingRequest pricingRequest = new PricingRequest();
            pricingRequest.setTenantId(request.getTenantId());
            pricingRequest.setStoreId(request.getStoreId());
            pricingRequest.setUserId(request.getUserId());
            pricingRequest.setMemberId(request.getMemberId());
            pricingRequest.setCouponId(request.getCouponId());
            pricingRequest.setUsePoints(request.getUsePoints());
            pricingRequest.setDeliveryMode(request.getDeliveryType());
            pricingRequest.setDeliveryDistance(request.getDeliveryDistance());
            pricingRequest.setOrderType(request.getOrderType());
            pricingRequest.setChannel(request.getChannel());
            pricingRequest.setEnableRounding(request.getEnableRounding());
            
            // 转换商品列表
            List<PricingItem> pricingItems = request.getItems().stream()
                    .map(item -> {
                        PricingItem pricingItem = new PricingItem();
                        pricingItem.setSkuId(item.getSkuId());
                        pricingItem.setSkuName("SKU-" + item.getSkuId()); // TODO: 从商品服务获取
                        pricingItem.setQuantity(item.getQuantity());
                        pricingItem.setBasePrice(item.getClientUnitPrice() != null ? item.getClientUnitPrice() : BigDecimal.ZERO);
                        pricingItem.setSpecSurcharge(BigDecimal.ZERO); // TODO: 从商品服务获取规格加价
                        return pricingItem;
                    })
                    .collect(Collectors.toList());
            pricingRequest.setItems(pricingItems);
            
            // 调用计价引擎
            return pricingFacade.quote(pricingRequest);
        } catch (Exception e) {
            log.error("计价失败", e);
            throw new BusinessException(OrderErrorCode.ORDER_INVALID, "计价失败: " + e.getMessage());
        }
    }
    
    /**
     * 计算总优惠金额
     */
    private BigDecimal calculateTotalDiscount(PricingQuote quote) {
        BigDecimal total = BigDecimal.ZERO;
        if (quote.getMemberDiscountAmount() != null) {
            total = total.add(quote.getMemberDiscountAmount());
        }
        if (quote.getPromoDiscountAmount() != null) {
            total = total.add(quote.getPromoDiscountAmount());
        }
        if (quote.getCouponDiscountAmount() != null) {
            total = total.add(quote.getCouponDiscountAmount());
        }
        if (quote.getPointsDiscountAmount() != null) {
            total = total.add(quote.getPointsDiscountAmount());
        }
        return total;
    }

    /**
     * 校验请求参数。
     */
    private void validateRequest(OrderConfirmRequest request) {
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
     * 从计价结果构建订单明细响应列表
     */
    private List<OrderConfirmItemResponse> buildItemResponsesFromQuote(OrderConfirmRequest request, PricingQuote quote) {
        return request.getItems().stream()
                .map(item -> {
                    BigDecimal unitPrice = item.getClientUnitPrice() != null
                            ? item.getClientUnitPrice()
                            : BigDecimal.ZERO;
                    BigDecimal discountAmount = BigDecimal.ZERO; // TODO: 从 quote 的 breakdownLines 中提取单品优惠
                    BigDecimal payableAmount = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()))
                            .subtract(discountAmount);

                    return OrderConfirmItemResponse.builder()
                            .skuId(item.getSkuId())
                            .productId(item.getProductId())
                            .productName("商品名称-" + item.getSkuId()) // TODO: 从商品服务获取
                            .skuName("SKU名称-" + item.getSkuId()) // TODO: 从商品服务获取
                            .productCode("CODE-" + item.getSkuId()) // TODO: 从商品服务获取
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
     * <p>规则：MD5(tenantId + storeId + userId + items + priceVersion + payableAmount)</p>
     * <p>用于后续提交单校验，防止价格篡改。</p>
     */
    private String generateConfirmToken(OrderConfirmRequest request, String priceVersion, BigDecimal payableAmount) {
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
