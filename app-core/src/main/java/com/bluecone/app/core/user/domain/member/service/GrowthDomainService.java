package com.bluecone.app.core.user.domain.member.service;

/**
 * 成长值与等级联动服务。
 */
public interface GrowthDomainService {

    void increaseGrowthAndCheckLevel(Long tenantId,
                                     Long memberId,
                                     int deltaGrowth,
                                     String bizType,
                                     String bizId);
}
