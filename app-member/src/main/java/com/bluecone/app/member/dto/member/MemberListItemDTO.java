package com.bluecone.app.user.dto.member;

import lombok.Data;

import java.util.List;

/**
 * 会员列表项 DTO，面向管理后台。
 */
@Data
public class MemberListItemDTO {
    private Long memberId;
    private Long tenantId;
    private Long userId;

    private String memberNo;

    private Integer status;
    private String statusLabel;

    private String nickname;
    private String avatarUrl;
    private String phoneMasked;

    private Long levelId;
    private String levelName;
    private Integer growthValue;

    private List<String> tagNames;

    private String joinAt;
    private String lastLoginAt;
}
