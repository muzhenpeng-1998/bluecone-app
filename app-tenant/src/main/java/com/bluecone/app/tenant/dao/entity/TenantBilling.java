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

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "套餐ID")
    private Long planId;

    @Schema(description = "支付金额")
    private BigDecimal payAmount;

    @Schema(description = "支付方式：wechatpay / alipay 等")
    private String payMethod;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "套餐到期时间")
    private LocalDateTime expireAt;

    @Schema(description = "状态：1成功，0失败")
    private Byte status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
