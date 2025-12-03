package com.bluecone.app.tenant.dao.service.impl;

import com.bluecone.app.tenant.dao.entity.TenantProfile;
import com.bluecone.app.tenant.dao.mapper.TenantProfileMapper;
import com.bluecone.app.tenant.dao.service.ITenantProfileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 租户主体详细资料 服务实现类
 * </p>
 *
 * @author muzhenpeng
 * @since 2025-12-03
 */
@Service
public class TenantProfileServiceImpl extends ServiceImpl<TenantProfileMapper, TenantProfile> implements ITenantProfileService {

}
