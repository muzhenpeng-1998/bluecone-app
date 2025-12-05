package com.bluecone.app.core.user.domain.event;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 优惠券发放事件。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponGrantedEvent {

    private Long tenantId;

    private Long memberId;

    private Long couponId;

    private Long couponTemplateId;

    private LocalDateTime occurredAt;
}
