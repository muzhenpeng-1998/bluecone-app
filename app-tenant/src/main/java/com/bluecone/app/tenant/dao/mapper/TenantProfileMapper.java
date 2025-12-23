package com.bluecone.app.tenant.dao.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.bluecone.app.tenant.dao.entity.TenantProfile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 租户主体详细资料 Mapper 接口
 * </p>
 *
 * @author muzhenpeng
 * @since 2025-12-03
 */
@Mapper
public interface TenantProfileMapper extends BaseMapper<TenantProfile> {

}
