package com.bluecone.app.core.user.domain.repository;

import java.util.Optional;

import com.bluecone.app.core.user.domain.member.TenantMember;

/**
 * 会员关系仓储接口。
 */
public interface TenantMemberRepository {

    Optional<TenantMember> findById(Long id);

    Optional<TenantMember> findByTenantAndUser(Long tenantId, Long userId);

    TenantMember save(TenantMember member);
}
