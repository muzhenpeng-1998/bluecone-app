package com.bluecone.app.tenant.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Data;

/**
 * 租户套餐/订阅记录
 *
 * @author muzhenpeng
 * @since 2025-12-03
 */
@Data
@Schema(name = "TenantBilling", description = "租户套餐/订阅记录")
public class TenantBilling implements Serializable {
    private static final long serialVersionUID = 1L;

    // 主键 ID，自增
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 租户 ID，关联 tenant.id
    @Schema(description = "租户ID")
    private Long tenantId;

    // 套餐 ID，关联 tenant_plan.id
    @Schema(description = "套餐ID")
    private Long planId;

    // 支付金额
    @Schema(description = "支付金额")
    private BigDecimal payAmount;

    // 支付方式：wechatpay / alipay 等
    @Schema(description = "支付方式：wechatpay / alipay 等")
    private String payMethod;

    // 支付时间
    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    // 套餐到期时间
    @Schema(description = "套餐到期时间")
    private LocalDateTime expireAt;

    // 记录状态：1成功，0失败
    @Schema(description = "状态：1成功，0失败")
    private Byte status;

    // 记录创建时间
    private LocalDateTime createdAt;

    // 记录更新时间
    private LocalDateTime updatedAt;

}
