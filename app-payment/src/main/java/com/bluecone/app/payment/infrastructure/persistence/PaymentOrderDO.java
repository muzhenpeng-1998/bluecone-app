package com.bluecone.app.payment.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 支付单数据对象，对应表 {@code bc_payment_order}。
 */
@Data
@TableName("bc_payment_order")
public class PaymentOrderDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long storeId;

    private Long businessOrderId;

    private Long userId;

    private String payChannel;

    private String payScene;

    private String currency;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    private BigDecimal discountAmount;

    private String status;

    private String clientIp;

    private String userAgent;

    private LocalDateTime expireTime;

    private String thirdAppId;

    private String thirdMchId;

    private String thirdSubMchId;

    private String thirdTradeNo;

    private Integer notifySuccess;

    @Version
    private Long version;

    private LocalDateTime createdAt;

    private Long createdBy;

    private LocalDateTime updatedAt;

    private Long updatedBy;
}
