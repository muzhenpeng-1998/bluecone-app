package com.bluecone.app.user.dto.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会员等级 DTO，对应 bc_member_level。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberLevelDTO {

    private Long id;

    private Long tenantId;

    private String levelCode;

    private String levelName;

    private Integer minGrowth;

    private Integer maxGrowth;

    private String benefitsJson;

    private Integer sortOrder;

    private Integer status;
}
