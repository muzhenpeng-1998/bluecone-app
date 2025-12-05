package com.bluecone.app.core.user.domain.repository;

import java.util.List;
import java.util.Optional;

import com.bluecone.app.core.user.domain.member.MemberLevel;

/**
 * 会员等级配置仓储接口。
 */
public interface MemberLevelRepository {

    Optional<MemberLevel> findById(Long id);

    Optional<MemberLevel> findByTenantAndCode(Long tenantId, String levelCode);

    List<MemberLevel> findByTenant(Long tenantId);

    List<MemberLevel> findEnabledByTenant(Long tenantId);

    MemberLevel save(MemberLevel level);
}
