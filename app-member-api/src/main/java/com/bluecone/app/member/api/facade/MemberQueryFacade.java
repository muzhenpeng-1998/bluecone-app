package com.bluecone.app.member.api.facade;

import com.bluecone.app.member.api.dto.MemberDTO;
import com.bluecone.app.member.api.dto.PointsBalanceDTO;

/**
 * 会员查询门面接口
 * 提供会员基础信息和积分查询能力
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public interface MemberQueryFacade {
    
    /**
     * 获取或创建会员（幂等）
     * 如果会员不存在，则自动创建；如果已存在，则返回现有会员
     * 
     * @param tenantId 租户ID
     * @param userId 平台用户ID
     * @return 会员信息
     */
    MemberDTO getOrCreateMember(Long tenantId, Long userId);
    
    /**
     * 根据会员ID查询会员信息
     * 
     * @param tenantId 租户ID
     * @param memberId 会员ID
     * @return 会员信息，不存在则返回 null
     */
    MemberDTO getMemberById(Long tenantId, Long memberId);
    
    /**
     * 根据用户ID查询会员信息
     * 
     * @param tenantId 租户ID
     * @param userId 平台用户ID
     * @return 会员信息，不存在则返回 null
     */
    MemberDTO getMemberByUserId(Long tenantId, Long userId);
    
    /**
     * 查询会员积分余额
     * 
     * @param tenantId 租户ID
     * @param memberId 会员ID
     * @return 积分余额（包含可用积分和冻结积分）
     */
    PointsBalanceDTO getPointsBalance(Long tenantId, Long memberId);
}
