package com.bluecone.app.tenant.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bluecone.app.tenant.dao.entity.TenantPlatformAccount;
import com.bluecone.app.tenant.dao.mapper.TenantPlatformAccountMapper;
import com.bluecone.app.tenant.dao.service.ITenantPlatformAccountService;
import org.springframework.stereotype.Service;

/**
 * 租户平台账号 服务实现类。
 */
@Service
public class TenantPlatformAccountServiceImpl extends ServiceImpl<TenantPlatformAccountMapper, TenantPlatformAccount>
        implements ITenantPlatformAccountService {
}
