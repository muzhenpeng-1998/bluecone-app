package com.bluecone.app.tenant.dao.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.bluecone.app.tenant.dao.entity.TenantBilling;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 租户套餐/订阅记录 Mapper 接口
 * </p>
 *
 * @author muzhenpeng
 * @since 2025-12-03
 */
@Mapper
public interface TenantBillingMapper extends BaseMapper<TenantBilling> {

}
