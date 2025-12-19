package com.bluecone.app.promo.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.promo.infra.persistence.po.CouponLockPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 优惠券锁定记录Mapper
 */
@Mapper
@Repository
public interface CouponLockMapper extends BaseMapper<CouponLockPO> {
    
    /**
     * 查询指定订单的所有优惠券锁定记录（用于运维诊断）
     * 
     * @param tenantId 租户ID
     * @param orderId 订单ID
     * @param limit 限制数量
     * @return 锁定记录列表
     */
    @Select("SELECT * FROM bc_coupon_lock " +
            "WHERE tenant_id = #{tenantId} " +
            "AND order_id = #{orderId} " +
            "ORDER BY created_at ASC " +
            "LIMIT #{limit}")
    List<CouponLockPO> selectByOrderId(@Param("tenantId") Long tenantId,
                                       @Param("orderId") Long orderId,
                                       @Param("limit") int limit);
}
