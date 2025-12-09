package com.bluecone.app.payment.simple.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 小程序调试支付单的数据库映射，当前复用 bc_payment_order 表。
 */
@Data
@TableName("bc_payment_order")
public class PaymentOrderDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long storeId;

    private Long userId;

    private Long orderId;

    private String payOrderNo;

    private Long totalAmount;

    private Long paidAmount;

    private String channel;

    private String status;

    private String outTransactionNo;

    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    private LocalDateTime updatedAt;
}
