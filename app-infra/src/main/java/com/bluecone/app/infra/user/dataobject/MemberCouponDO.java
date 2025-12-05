package com.bluecone.app.infra.user.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员优惠券实例表映射，表名：bc_member_coupon。
 */
@Data
@TableName("bc_member_coupon")
public class MemberCouponDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long memberId;

    private Long couponTemplateId;

    private String couponCode;

    private Integer status;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private String usedOrderNo;

    private LocalDateTime usedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
