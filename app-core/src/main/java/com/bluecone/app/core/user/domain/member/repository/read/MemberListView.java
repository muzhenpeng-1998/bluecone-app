package com.bluecone.app.core.user.domain.member.repository.read;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会员列表视图对象（领域读模型）。
 */
@Data
public class MemberListView {
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
    private LocalDateTime joinAt;
    private LocalDateTime lastLoginAt;
}
