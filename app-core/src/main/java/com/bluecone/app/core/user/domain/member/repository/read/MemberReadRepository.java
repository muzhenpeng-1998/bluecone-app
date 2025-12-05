package com.bluecone.app.core.user.domain.member.repository.read;

/**
 * 会员读模型仓储，提供多表查询能力。
 */
public interface MemberReadRepository {

    PageResult<MemberListView> searchMembers(MemberSearchQuery query);
}
