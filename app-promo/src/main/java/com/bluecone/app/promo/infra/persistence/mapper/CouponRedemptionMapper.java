package com.bluecone.app.promo.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.promo.infra.persistence.po.CouponRedemptionPO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 优惠券核销记录Mapper
 */
@Mapper
@Repository
public interface CouponRedemptionMapper extends BaseMapper<CouponRedemptionPO> {
}
