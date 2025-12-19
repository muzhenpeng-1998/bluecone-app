package com.bluecone.app.promo.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.promo.infra.persistence.po.CouponLockPO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 优惠券锁定记录Mapper
 */
@Mapper
@Repository
public interface CouponLockMapper extends BaseMapper<CouponLockPO> {
}
