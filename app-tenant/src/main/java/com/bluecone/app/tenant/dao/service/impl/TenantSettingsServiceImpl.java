package com.bluecone.app.tenant.dao.service.impl;

import com.bluecone.app.tenant.dao.entity.TenantSettings;
import com.bluecone.app.tenant.dao.mapper.TenantSettingsMapper;
import com.bluecone.app.tenant.dao.service.ITenantSettingsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 租户配置项（KV 模型） 服务实现类
 * </p>
 *
 * @author muzhenpeng
 * @since 2025-12-03
 */
@Service
public class TenantSettingsServiceImpl extends ServiceImpl<TenantSettingsMapper, TenantSettings> implements ITenantSettingsService {

}
