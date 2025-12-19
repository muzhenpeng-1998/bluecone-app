package com.bluecone.app.member.domain.repository;

import com.bluecone.app.member.domain.model.Member;

import java.util.Optional;

/**
 * 会员仓储接口
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public interface MemberRepository {
    
    /**
     * 根据ID查询会员
     * 
     * @param tenantId 租户ID
     * @param memberId 会员ID
     * @return 会员信息
     */
    Optional<Member> findById(Long tenantId, Long memberId);
    
    /**
     * 根据用户ID查询会员
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 会员信息
     */
    Optional<Member> findByUserId(Long tenantId, Long userId);
    
    /**
     * 保存会员
     * 
     * @param member 会员信息
     */
    void save(Member member);
    
    /**
     * 更新会员
     * 
     * @param member 会员信息
     */
    void update(Member member);
}
