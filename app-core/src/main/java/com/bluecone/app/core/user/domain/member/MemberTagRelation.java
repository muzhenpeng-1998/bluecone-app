package com.bluecone.app.core.user.domain.member;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 会员-标签关联的简单值对象，对应表 bc_member_tag_relation。
 */
@Data
public class MemberTagRelation {

    private Long id;

    private Long tenantId;

    private Long memberId;

    private Long tagId;

    private LocalDateTime createdAt;
}
