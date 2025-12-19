package com.bluecone.app.member.api.impl;

import com.bluecone.app.member.api.dto.MemberDTO;
import com.bluecone.app.member.api.dto.PointsBalanceDTO;
import com.bluecone.app.member.api.facade.MemberQueryFacade;
import com.bluecone.app.member.application.service.MemberApplicationService;
import com.bluecone.app.member.application.service.PointsApplicationService;
import com.bluecone.app.member.domain.model.Member;
import com.bluecone.app.member.domain.model.PointsAccount;
import org.springframework.stereotype.Service;

/**
 * 会员查询门面实现
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Service
public class MemberQueryFacadeImpl implements MemberQueryFacade {
    
    private final MemberApplicationService memberApplicationService;
    private final PointsApplicationService pointsApplicationService;
    
    public MemberQueryFacadeImpl(MemberApplicationService memberApplicationService,
                                PointsApplicationService pointsApplicationService) {
        this.memberApplicationService = memberApplicationService;
        this.pointsApplicationService = pointsApplicationService;
    }
    
    @Override
    public MemberDTO getOrCreateMember(Long tenantId, Long userId) {
        Member member = memberApplicationService.getOrCreateMember(tenantId, userId);
        return toMemberDTO(member);
    }
    
    @Override
    public MemberDTO getMemberById(Long tenantId, Long memberId) {
        Member member = memberApplicationService.getMemberById(tenantId, memberId);
        return toMemberDTO(member);
    }
    
    @Override
    public MemberDTO getMemberByUserId(Long tenantId, Long userId) {
        Member member = memberApplicationService.getMemberByUserId(tenantId, userId);
        return toMemberDTO(member);
    }
    
    @Override
    public PointsBalanceDTO getPointsBalance(Long tenantId, Long memberId) {
        PointsAccount account = pointsApplicationService.getPointsBalance(tenantId, memberId);
        if (account == null) {
            return null;
        }
        return new PointsBalanceDTO(
                account.getMemberId(),
                account.getTenantId(),
                account.getAvailablePoints(),
                account.getFrozenPoints()
        );
    }
    
    /**
     * 转换为 MemberDTO
     */
    private MemberDTO toMemberDTO(Member member) {
        if (member == null) {
            return null;
        }
        
        MemberDTO dto = new MemberDTO();
        dto.setMemberId(member.getId());
        dto.setTenantId(member.getTenantId());
        dto.setUserId(member.getUserId());
        dto.setMemberNo(member.getMemberNo());
        dto.setStatus(member.getStatus().name());
        dto.setCreatedAt(member.getCreatedAt());
        dto.setUpdatedAt(member.getUpdatedAt());
        return dto;
    }
}
