package com.bluecone.app.order.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小程序用户侧确认订单预览请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmOrderPreviewRequest {

    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    @NotNull(message = "门店ID不能为空")
    private Long storeId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    @NotBlank(message = "订单来源不能为空")
    private String orderSource;

    @NotBlank(message = "下单渠道不能为空")
    private String channel;

    @NotEmpty(message = "订单明细不能为空")
    @Valid
    private List<ConfirmOrderItemDTO> items;

    private Long couponId;

    private BigDecimal pointsAmount;

    private BigDecimal clientPayableAmount;

    private String sessionId;

    private Integer sessionVersion;

    private String remark;

    /**
     * 扩展字段 JSON 字符串。
     */
    private String ext;
}
