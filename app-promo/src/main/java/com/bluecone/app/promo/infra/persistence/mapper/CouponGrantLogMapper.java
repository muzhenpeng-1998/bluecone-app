package com.bluecone.app.promo.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.promo.infra.persistence.po.CouponGrantLogPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 优惠券发放日志Mapper
 */
@Mapper
public interface CouponGrantLogMapper extends BaseMapper<CouponGrantLogPO> {

    /**
     * 统计用户从指定模板领取的券数量
     */
    @Select("SELECT COUNT(*) FROM bc_coupon_grant_log " +
            "WHERE tenant_id = #{tenantId} " +
            "AND template_id = #{templateId} " +
            "AND user_id = #{userId} " +
            "AND grant_status = 'SUCCESS'")
    int countUserGrantedByTemplate(@Param("tenantId") Long tenantId,
                                    @Param("templateId") Long templateId,
                                    @Param("userId") Long userId);
}
