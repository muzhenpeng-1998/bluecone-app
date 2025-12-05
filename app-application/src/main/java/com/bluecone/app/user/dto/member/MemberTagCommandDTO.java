package com.bluecone.app.user.dto.member;

import lombok.Data;

import java.util.List;

/**
 * 会员打标/去标命令。
 */
@Data
public class MemberTagCommandDTO {

    private Long tenantId;

    private List<Long> memberIds;

    private List<Long> tagIds;
}
