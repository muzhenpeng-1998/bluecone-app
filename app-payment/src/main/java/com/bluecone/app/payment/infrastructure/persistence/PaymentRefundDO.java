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
 * 支付退款单数据对象，对应表 {@code bc_payment_refund}。
 */
@Data
@TableName("bc_payment_refund")
public class PaymentRefundDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long paymentOrderId;

    private String refundNo;

    private String thirdRefundNo;

    private BigDecimal refundAmount;

    private String status;

    private String reason;

    private Integer notifySuccess;

    @Version
    private Long version;

    private LocalDateTime createdAt;

    private Long createdBy;

    private LocalDateTime updatedAt;

    private Long updatedBy;
}
