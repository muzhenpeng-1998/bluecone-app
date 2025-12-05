package com.bluecone.app.core.user.domain.repository;

import com.bluecone.app.core.user.domain.member.MemberTag;

import java.util.List;
import java.util.Map;

/**
 * 会员标签关联仓储接口。
 */
public interface MemberTagRelationRepository {

    void addTagToMember(Long tenantId, Long memberId, Long tagId);

    void removeTagFromMember(Long tenantId, Long memberId, Long tagId);

    List<MemberTag> findTagsByMember(Long tenantId, Long memberId);

    Map<Long, List<MemberTag>> findTagsByMemberIds(Long tenantId, List<Long> memberIds);
}
