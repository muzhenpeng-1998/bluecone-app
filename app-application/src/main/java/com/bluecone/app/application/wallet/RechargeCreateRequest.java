package com.bluecone.app.application.wallet;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 充值创建请求
 * 
 * @author bluecone
 * @since 2025-12-19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "充值创建请求")
public class RechargeCreateRequest {
    
    @Schema(description = "租户ID（可选，默认为1）", example = "1")
    private Long tenantId;
    
    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID", example = "123", required = true)
    private Long userId;
    
    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于0.01元")
    @Schema(description = "充值金额（单位：元）", example = "100.00", required = true)
    private BigDecimal rechargeAmount;
    
    @NotBlank(message = "支付渠道不能为空")
    @Schema(description = "支付渠道（WECHAT/ALIPAY）", example = "WECHAT", required = true)
    private String payChannel;
    
    @NotBlank(message = "幂等键不能为空")
    @Schema(description = "幂等键（客户端生成UUID，用于防重）", example = "uuid-123456", required = true)
    private String idempotencyKey;
}
