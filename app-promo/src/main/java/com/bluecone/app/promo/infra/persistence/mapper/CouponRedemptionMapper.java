package com.bluecone.app.promo.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.promo.infra.persistence.po.CouponRedemptionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 优惠券核销记录Mapper
 */
@Mapper
@Repository
public interface CouponRedemptionMapper extends BaseMapper<CouponRedemptionPO> {
    
    /**
     * 查询指定订单的所有优惠券核销记录（用于运维诊断）
     * 
     * @param tenantId 租户ID
     * @param orderId 订单ID
     * @param limit 限制数量
     * @return 核销记录列表
     */
    @Select("SELECT * FROM bc_coupon_redemption " +
            "WHERE tenant_id = #{tenantId} " +
            "AND order_id = #{orderId} " +
            "ORDER BY created_at ASC " +
            "LIMIT #{limit}")
    List<CouponRedemptionPO> selectByOrderId(@Param("tenantId") Long tenantId,
                                             @Param("orderId") Long orderId,
                                             @Param("limit") int limit);
}
