package com.bluecone.app.promo.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.promo.infra.persistence.po.CouponPO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 优惠券实例Mapper
 */
@Mapper
@Repository
public interface CouponMapper extends BaseMapper<CouponPO> {
}
