package com.bluecone.app.order.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 确认订单的单条明细项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmOrderItemDTO {

    /**
     * 商品ID，可选，部分自定义项可能为空。
     */
    private Long productId;

    /**
     * SKU ID，可选。
     */
    private Long skuId;

    /**
     * 商品名称（前端展示用，可以冗余）。
     */
    @NotBlank
    @Size(max = 256)
    private String productName;

    /**
     * 规格名称，如大杯/中杯/500ml 等。
     */
    @Size(max = 256)
    private String skuName;

    /**
     * 商品编码/货号，可选。
     */
    @Size(max = 64)
    private String productCode;

    /**
     * 数量，必须大于 0。
     */
    @NotNull
    @Min(1)
    private Integer quantity;

    /**
     * 前端理解的单价（仅用于与服务端核对，不作为最终金额来源）。
     */
    private BigDecimal clientUnitPrice;

    /**
     * 前端预估的小计金额（clientUnitPrice * quantity，只用于对账）。
     */
    private BigDecimal clientSubtotalAmount;

    /**
     * 商品级备注，比如「半糖」「少冰」。
     */
    @Size(max = 256)
    private String remark;

    /**
     * 自定义属性，支持不同业态扩展。
     */
    @Builder.Default
    private Map<String, Object> attrs = Collections.emptyMap();
}
