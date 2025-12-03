package com.bluecone.app.tenant.dao.service.impl;

import com.bluecone.app.tenant.dao.entity.Tenant;
import com.bluecone.app.tenant.dao.mapper.TenantMapper;
import com.bluecone.app.tenant.dao.service.ITenantService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 租户表 服务实现类
 * </p>
 *
 * @author muzhenpeng
 * @since 2025-12-01
 */
@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements ITenantService {

}
