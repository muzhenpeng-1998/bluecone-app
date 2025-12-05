package com.bluecone.app.user.dto.member;

import lombok.Data;

import java.util.List;

/**
 * 会员列表查询条件 DTO。
 */
@Data
public class MemberSearchQueryDTO {

    private Long tenantId;
    private String keyword;

    private Long levelId;
    private Integer status;
    private List<Long> tagIds;

    private Integer minGrowth;
    private Integer maxGrowth;

    private String joinStartDate;
    private String joinEndDate;

    private Integer pageNo;
    private Integer pageSize;
}
