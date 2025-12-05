package com.bluecone.app.infra.user.query;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员列表视图 DO，用于多表查询结果映射。
 */
@Data
public class MemberListViewDO {

    private Long memberId;
    private Long tenantId;
    private Long userId;
    private String memberNo;
    private Integer status;
    private String nickname;
    private String avatarUrl;
    private String phone;
    private Long levelId;
    private String levelName;
    private Integer growthValue;
    private LocalDateTime joinAt;
    private LocalDateTime lastLoginAt;
}
