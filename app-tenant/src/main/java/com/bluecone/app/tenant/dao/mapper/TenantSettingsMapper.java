package com.bluecone.app.tenant.dao.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.bluecone.app.tenant.dao.entity.TenantSettings;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 租户配置项（KV 模型） Mapper 接口
 * </p>
 *
 * @author muzhenpeng
 * @since 2025-12-03
 */
@Mapper
public interface TenantSettingsMapper extends BaseMapper<TenantSettings> {

}
