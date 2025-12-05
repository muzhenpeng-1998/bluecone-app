package com.bluecone.app.core.user.domain.member;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 会员等级配置，对应表 bc_member_level。
 */
@Data
public class MemberLevel {

    private Long id;

    private Long tenantId;

    private String levelCode;

    private String levelName;

    private int minGrowth;

    private int maxGrowth;

    private String benefitsJson;

    private int sortOrder;

    private int status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 判断成长值是否落在本等级区间。
     */
    public boolean matchesGrowth(int growthValue) {
        return growthValue >= minGrowth && growthValue <= maxGrowth;
    }
}
