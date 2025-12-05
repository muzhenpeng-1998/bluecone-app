package com.bluecone.app.infra.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.user.dataobject.MemberCouponDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * bc_member_coupon 表对应的 Mapper。
 */
@Mapper
public interface MemberCouponMapper extends BaseMapper<MemberCouponDO> {
}
