package com.bluecone.app.growth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.growth.infrastructure.persistence.po.AttributionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 归因关系Mapper
 */
@Mapper
public interface AttributionMapper extends BaseMapper<AttributionPO> {
    
    /**
     * 统计用户在租户下的已支付订单数
     * 用于判断是否首单
     */
    @Select("SELECT COUNT(DISTINCT o.id) FROM bc_order o " +
            "WHERE o.tenant_id = #{tenantId} " +
            "AND o.user_id = #{userId} " +
            "AND o.pay_status = 'PAID'")
    int countPaidOrdersByUser(@Param("tenantId") Long tenantId, 
                              @Param("userId") Long userId);
}
