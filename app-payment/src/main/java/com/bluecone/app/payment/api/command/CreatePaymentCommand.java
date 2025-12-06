package com.bluecone.app.payment.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 创建支付单的命令对象。
 * <p>
 * - 由上层（前端/订单模块）传递，应用层使用 @Valid 进行校验；
 * - 支持基本幂等与金额校验。
 */
@Data
public class CreatePaymentCommand {

    @NotNull
    private Long tenantId;

    @NotNull
    private Long storeId;

    @NotNull
    private Long userId;

    @NotBlank
    private String bizType;

    @NotBlank
    private String bizOrderNo;

    @NotBlank
    private String channelCode;  // PaymentChannel.code

    @NotBlank
    private String methodCode;   // PaymentMethod.code

    @NotBlank
    private String sceneCode;    // PaymentScene.code

    @NotNull
    private BigDecimal totalAmount;

    @NotNull
    private BigDecimal discountAmount;

    private String currency;

    /**
     * 幂等 Key：同一业务订单重复调用时应保持一致。
     */
    @NotBlank
    private String idempotentKey;

    /**
     * 支付超时时间（分钟）。
     * - 为空则使用默认 15 分钟；
     * - 若不为空，需 > 0。
     */
    private Integer expireMinutes;
}
