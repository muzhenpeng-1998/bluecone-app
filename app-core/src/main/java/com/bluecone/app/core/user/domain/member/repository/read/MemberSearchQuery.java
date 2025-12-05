package com.bluecone.app.core.user.domain.member.repository.read;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会员列表查询条件（领域层）。
 */
@Data
public class MemberSearchQuery {
    private Long tenantId;
    private String keyword;
    private Long levelId;
    private Integer status;
    private List<Long> tagIds;
    private Integer minGrowth;
    private Integer maxGrowth;
    private LocalDateTime joinStart;
    private LocalDateTime joinEnd;
    private int pageNo = 1;
    private int pageSize = 20;
}
