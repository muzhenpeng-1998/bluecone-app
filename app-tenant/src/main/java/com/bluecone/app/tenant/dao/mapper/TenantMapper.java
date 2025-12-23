package com.bluecone.app.tenant.dao.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.bluecone.app.tenant.dao.entity.Tenant;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 租户表 Mapper 接口
 * </p>
 *
 * @author muzhenpeng
 * @since 2025-12-01
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {

}
