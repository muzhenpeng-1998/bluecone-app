package com.bluecone.app.core.user.domain.repository;

import java.util.List;
import java.util.Optional;

import com.bluecone.app.core.user.domain.account.MemberCoupon;

/**
 * 会员优惠券仓储接口。
 */
public interface CouponRepository {

    Optional<MemberCoupon> findById(Long id);

    Optional<MemberCoupon> findByTenantAndCode(Long tenantId, String couponCode);

    List<MemberCoupon> findByMember(Long tenantId, Long memberId);

    MemberCoupon save(MemberCoupon coupon);
}
