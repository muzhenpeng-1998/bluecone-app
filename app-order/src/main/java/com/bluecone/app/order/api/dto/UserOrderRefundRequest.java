package com.bluecone.app.order.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 用户发起退款申请请求。
 */
@Data
public class UserOrderRefundRequest {

    @NotNull(message = "tenantId 不能为空")
    private Long tenantId;

    @NotNull(message = "userId 不能为空")
    private Long userId;

    /**
     * FULL / PARTIAL。
     */
    @NotBlank(message = "refundType 不能为空")
    private String refundType;

    /**
     * 部分退款时必填。
     */
    private BigDecimal refundAmount;

    /**
     * 退款原因编码。
     */
    @NotBlank(message = "reasonCode 不能为空")
    private String reasonCode;

    /**
     * 退款原因描述。
     */
    @Size(max = 200, message = "reasonRemark 长度不能超过200")
    private String reasonRemark;
}
