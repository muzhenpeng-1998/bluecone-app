package com.bluecone.app.core.user.domain.account;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 会员优惠券实例，对应表 bc_member_coupon。
 */
@Data
public class MemberCoupon {

    private Long id;

    private Long tenantId;

    private Long memberId;

    private Long couponTemplateId;

    private String couponCode;

    /** 券状态：1未使用,2已使用,3已过期,4已作废 */
    private int status;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private String usedOrderNo;

    private LocalDateTime usedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
