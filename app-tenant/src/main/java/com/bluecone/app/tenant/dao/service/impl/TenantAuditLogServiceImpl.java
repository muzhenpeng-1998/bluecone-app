package com.bluecone.app.tenant.dao.service.impl;

import com.bluecone.app.tenant.dao.entity.TenantAuditLog;
import com.bluecone.app.tenant.dao.mapper.TenantAuditLogMapper;
import com.bluecone.app.tenant.dao.service.ITenantAuditLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 租户审计日志 服务实现类
 * </p>
 *
 * @author muzhenpeng
 * @since 2025-12-03
 */
@Service
public class TenantAuditLogServiceImpl extends ServiceImpl<TenantAuditLogMapper, TenantAuditLog> implements ITenantAuditLogService {

}


