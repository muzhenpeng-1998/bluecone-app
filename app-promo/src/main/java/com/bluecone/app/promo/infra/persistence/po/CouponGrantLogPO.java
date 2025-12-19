package com.bluecone.app.promo.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券发放日志表PO
 */
@Data
@TableName("bc_coupon_grant_log")
public class CouponGrantLogPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private Long tenantId;

    private Long templateId;

    private String idempotencyKey;

    private Long userId;

    private Long couponId;

    private String grantSource;

    private String grantStatus;

    private Long operatorId;

    private String operatorName;

    private String batchNo;

    private String grantReason;

    private String errorCode;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
