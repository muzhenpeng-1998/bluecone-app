package com.bluecone.app.infra.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.admin.entity.AdminAuditLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 后台审计日志Mapper
 */
@Mapper
public interface AdminAuditLogMapper extends BaseMapper<AdminAuditLogEntity> {
}
