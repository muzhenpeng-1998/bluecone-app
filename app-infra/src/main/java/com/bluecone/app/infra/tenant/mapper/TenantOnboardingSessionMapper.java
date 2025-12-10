package com.bluecone.app.infra.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.tenant.dataobject.TenantOnboardingSessionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * bc_tenant_onboarding_session 表对应的 Mapper。
 */
@Mapper
public interface TenantOnboardingSessionMapper extends BaseMapper<TenantOnboardingSessionDO> {
}

