package com.bluecone.app.core.user.domain.repository;

import java.util.List;
import java.util.Optional;

import com.bluecone.app.core.user.domain.member.MemberTag;

/**
 * 会员标签仓储接口。
 */
public interface MemberTagRepository {

    Optional<MemberTag> findById(Long id);

    Optional<MemberTag> findByTenantAndCode(Long tenantId, String tagCode);

    List<MemberTag> findByTenant(Long tenantId);

    MemberTag save(MemberTag tag);
}
