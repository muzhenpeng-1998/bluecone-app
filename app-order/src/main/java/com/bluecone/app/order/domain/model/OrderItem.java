package com.bluecone.app.order.domain.model;

import com.bluecone.app.order.api.dto.ConfirmOrderItemDTO;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orderId;

    private Long tenantId;

    private Long storeId;

    // 商品维度
    private Long productId;

    private Long skuId;

    private String productName;

    private String skuName;

    private String productCode;

    // 数量 & 金额
    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal discountAmount;

    private BigDecimal payableAmount;

    // 属性 & 备注
    @Builder.Default
    private Map<String, Object> attrs = Collections.emptyMap();

    private String remark;

    private LocalDateTime createdAt;

    private Long createdBy;

    private LocalDateTime updatedAt;

    private Long updatedBy;

    /**
     * 重新计算本条目的金额。
     */
    public void recalculateAmounts() {
        BigDecimal quantity = BigDecimal.valueOf(this.quantity == null ? 0 : this.quantity);
        BigDecimal gross = defaultBigDecimal(this.unitPrice).multiply(quantity);
        BigDecimal discount = defaultBigDecimal(this.discountAmount);
        BigDecimal payable = gross.subtract(discount);
        if (payable.compareTo(BigDecimal.ZERO) < 0) {
            payable = BigDecimal.ZERO;
        }
        this.payableAmount = payable;
    }

    /**
     * 判断是否与指定 SKU+属性为同一购物车行。
     */
    public boolean sameCartLine(Long skuId, Map<String, Object> attrs) {
        if (!Objects.equals(this.skuId, skuId)) {
            return false;
        }
        return mapEquals(this.attrs, attrs);
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean mapEquals(Map<String, Object> left, Map<String, Object> right) {
        if ((left == null || left.isEmpty()) && (right == null || right.isEmpty())) {
            return true;
        }
        return Objects.equals(left, right);
    }

    /**
     * 由确认订单 DTO 构建明细行。
     */
    public static OrderItem fromConfirmItem(ConfirmOrderItemDTO dto, Long tenantId, Long storeId, LocalDateTime now) {
        OrderItem item = new OrderItem();
        item.setProductId(dto.getProductId());
        item.setSkuId(dto.getSkuId());
        item.setTenantId(tenantId);
        item.setStoreId(storeId);
        item.setProductName(dto.getProductName());
        item.setSkuName(dto.getSkuName());
        item.setProductCode(dto.getProductCode());
        item.setQuantity(dto.getQuantity());
        item.setUnitPrice(defaultOrZero(dto.getClientUnitPrice()));
        item.setDiscountAmount(BigDecimal.ZERO);
        item.setAttrs(dto.getAttrs() == null ? Collections.emptyMap() : dto.getAttrs());
        item.setRemark(dto.getRemark());
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        item.recalculateAmounts();
        return item;
    }

    private static BigDecimal defaultOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
