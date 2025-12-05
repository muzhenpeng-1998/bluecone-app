package com.bluecone.app.user.dto.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 会员概要信息，用于展示当前租户下的会员状态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberSummaryDTO {

    private Long memberId;

    private Long tenantId;

    private Long userId;

    private String memberNo;

    private Integer status;

    private Long levelId;

    private String levelName;

    private Integer growthValue;

    private List<String> tags;
}
