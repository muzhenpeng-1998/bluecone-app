package com.bluecone.app.tenant.dao.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.bluecone.app.tenant.dao.entity.TenantAuditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 租户审计日志 Mapper 接口
 * </p>
 *
 * @author muzhenpeng
 * @since 2025-12-03
 */
@Mapper
public interface TenantAuditLogMapper extends BaseMapper<TenantAuditLog> {

}
