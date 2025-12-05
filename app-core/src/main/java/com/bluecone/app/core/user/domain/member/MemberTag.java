package com.bluecone.app.core.user.domain.member;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 会员标签定义，对应表 bc_member_tag。
 */
@Data
public class MemberTag {

    private Long id;

    private Long tenantId;

    private String tagCode;

    private String tagName;

    private String color;

    private int status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
