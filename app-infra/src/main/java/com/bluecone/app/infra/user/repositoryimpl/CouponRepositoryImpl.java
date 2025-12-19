package com.bluecone.app.infra.user.repositoryimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.user.domain.account.MemberCoupon;
import com.bluecone.app.core.user.domain.repository.CouponRepository;
import com.bluecone.app.infra.user.dataobject.MemberCouponDO;
import com.bluecone.app.infra.user.mapper.MemberCouponMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 优惠券仓储实现，对应表 bc_member_coupon。
 */
@Repository("coreMemberCouponRepositoryImpl")
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {

    private final MemberCouponMapper memberCouponMapper;

    @Override
    public Optional<MemberCoupon> findById(Long id) {
        return Optional.ofNullable(toDomain(memberCouponMapper.selectById(id)));
    }

    @Override
    public Optional<MemberCoupon> findByTenantAndCode(Long tenantId, String couponCode) {
        MemberCouponDO dataObject = memberCouponMapper.selectOne(new LambdaQueryWrapper<MemberCouponDO>()
                .eq(MemberCouponDO::getTenantId, tenantId)
                .eq(MemberCouponDO::getCouponCode, couponCode));
        return Optional.ofNullable(toDomain(dataObject));
    }

    @Override
    public List<MemberCoupon> findByMember(Long tenantId, Long memberId) {
        List<MemberCouponDO> dataObjects = memberCouponMapper.selectList(new LambdaQueryWrapper<MemberCouponDO>()
                .eq(MemberCouponDO::getTenantId, tenantId)
                .eq(MemberCouponDO::getMemberId, memberId));
        return dataObjects.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public MemberCoupon save(MemberCoupon coupon) {
        MemberCouponDO dataObject = toDO(coupon);
        if (coupon.getId() == null) {
            memberCouponMapper.insert(dataObject);
            coupon.setId(dataObject.getId());
        } else {
            memberCouponMapper.updateById(dataObject);
        }
        return coupon;
    }

    private MemberCoupon toDomain(MemberCouponDO dataObject) {
        if (dataObject == null) {
            return null;
        }
        MemberCoupon coupon = new MemberCoupon();
        coupon.setId(dataObject.getId());
        coupon.setTenantId(dataObject.getTenantId());
        coupon.setMemberId(dataObject.getMemberId());
        coupon.setCouponTemplateId(dataObject.getCouponTemplateId());
        coupon.setCouponCode(dataObject.getCouponCode());
        coupon.setStatus(dataObject.getStatus() != null ? dataObject.getStatus() : 0);
        coupon.setValidFrom(dataObject.getValidFrom());
        coupon.setValidTo(dataObject.getValidTo());
        coupon.setUsedOrderNo(dataObject.getUsedOrderNo());
        coupon.setUsedAt(dataObject.getUsedAt());
        coupon.setCreatedAt(dataObject.getCreatedAt());
        coupon.setUpdatedAt(dataObject.getUpdatedAt());
        return coupon;
    }

    private MemberCouponDO toDO(MemberCoupon coupon) {
        if (coupon == null) {
            return null;
        }
        MemberCouponDO dataObject = new MemberCouponDO();
        dataObject.setId(coupon.getId());
        dataObject.setTenantId(coupon.getTenantId());
        dataObject.setMemberId(coupon.getMemberId());
        dataObject.setCouponTemplateId(coupon.getCouponTemplateId());
        dataObject.setCouponCode(coupon.getCouponCode());
        dataObject.setStatus(coupon.getStatus());
        dataObject.setValidFrom(coupon.getValidFrom());
        dataObject.setValidTo(coupon.getValidTo());
        dataObject.setUsedOrderNo(coupon.getUsedOrderNo());
        dataObject.setUsedAt(coupon.getUsedAt());
        dataObject.setCreatedAt(coupon.getCreatedAt());
        dataObject.setUpdatedAt(coupon.getUpdatedAt());
        return dataObject;
    }
}
