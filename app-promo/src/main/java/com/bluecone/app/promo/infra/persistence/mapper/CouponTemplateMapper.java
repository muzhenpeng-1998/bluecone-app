package com.bluecone.app.promo.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.promo.infra.persistence.po.CouponTemplatePO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 优惠券模板Mapper
 */
@Mapper
@Repository
public interface CouponTemplateMapper extends BaseMapper<CouponTemplatePO> {
}
