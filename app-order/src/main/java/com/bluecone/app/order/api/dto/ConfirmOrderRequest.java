package com.bluecone.app.order.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 确认订单接口的请求模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmOrderRequest {

    // === 上下文信息 ===

    /**
     * 租户ID，多租户隔离必备。
     */
    @NotNull
    private Long tenantId;

    /**
     * 门店ID。
     */
    @NotNull
    private Long storeId;

    /**
     * 用户ID，C端登录用户；匿名用户可为空。
     */
    @NotNull
    private Long userId;

    /**
     * 下单渠道：WECHAT_MINI / ALIPAY_MINI / H5 / POS 等。
     */
    @NotBlank
    @Size(max = 32)
    private String channel;

    /**
     * 业务业态类型：COFFEE/FOOD/RETAIL/BEAUTY/VENUE 等。
     */
    @NotBlank
    @Size(max = 32)
    private String bizType;

    // === 一起点单 / 会话相关 ===

    /**
     * 一起点单会话ID，支持多人同桌点单。
     */
    @Size(max = 64)
    private String sessionId;

    /**
     * 桌台ID，堂食场景可用，其他业态可为空。
     */
    private Long tableId;

    /**
     * 会话版本号，前端持有的版本，用于一起点单时的乐观锁控制。
     */
    @Builder.Default
    private Integer sessionVersion = 0;

    // === 订单基础信息 ===

    /**
     * 客户端订单号/幂等ID（例如前端生成的UUID），用于防重复确认。
     */
    @NotBlank
    @Size(max = 64)
    private String clientOrderNo;

    /**
     * 订单场景：DINE_IN/TAKEAWAY/DELIVERY/BOOKING/RETAIL 等。
     */
    @NotBlank
    @Size(max = 32)
    private String orderSource;

    /**
     * 整单备注，比如「全部少辣」「客户生日」等。
     */
    @Size(max = 512)
    private String remark;

    /**
     * 订单明细项列表，不能为空。
     */
    @NotEmpty
    @Valid
    @Builder.Default
    private List<ConfirmOrderItemDTO> items = Collections.emptyList();

    // === 前端预估金额 ===

    /**
     * 前端计算的原价总额（所有明细原价之和）。
     */
    @NotNull
    @jakarta.validation.constraints.DecimalMin(value = "0.00")
    private BigDecimal clientTotalAmount;

    /**
     * 前端计算的优惠总额。
     */
    @NotNull
    @jakarta.validation.constraints.DecimalMin(value = "0.00")
    private BigDecimal clientDiscountAmount;

    /**
     * 前端认为的应付金额 = total - discount + 其他费用。
     */
    @NotNull
    @jakarta.validation.constraints.DecimalMin(value = "0.00")
    private BigDecimal clientPayableAmount;

    // === 支付意向 ===

    /**
     * 预期支付渠道：WECHAT_JSAPI/WECHAT_NATIVE/CASH/CARD 等。
     */
    @Size(max = 32)
    private String payChannel;

    /**
     * 是否在确认订单后立即创建支付单。
     */
    @Builder.Default
    private Boolean autoCreatePayment = Boolean.TRUE;

    // === 扩展字段 ===

    /**
     * 扩展字段，预留给不同业态，例如预约时间/服务时长等。
     */
    @Builder.Default
    private Map<String, Object> ext = Collections.emptyMap();
}
