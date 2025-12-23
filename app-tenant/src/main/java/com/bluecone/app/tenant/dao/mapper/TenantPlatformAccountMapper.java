package com.bluecone.app.tenant.dao.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.tenant.dao.entity.TenantPlatformAccount;

/**
 * 租户平台账号 Mapper。
 */
@Mapper
public interface TenantPlatformAccountMapper extends BaseMapper<TenantPlatformAccount> {
}
