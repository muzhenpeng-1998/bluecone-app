package com.bluecone.app.member.infra.converter;

import com.bluecone.app.member.domain.enums.MemberStatus;
import com.bluecone.app.member.domain.model.Member;
import com.bluecone.app.member.infra.persistence.po.MemberPO;

/**
 * 会员实体与PO转换器
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public class MemberConverter {
    
    /**
     * PO 转 Domain
     */
    public static Member toDomain(MemberPO po) {
        if (po == null) {
            return null;
        }
        
        Member member = new Member();
        member.setId(po.getId());
        member.setTenantId(po.getTenantId());
        member.setUserId(po.getUserId());
        member.setMemberNo(po.getMemberNo());
        member.setStatus(MemberStatus.valueOf(po.getStatus()));
        member.setCreatedAt(po.getCreatedAt());
        member.setCreatedBy(po.getCreatedBy());
        member.setUpdatedAt(po.getUpdatedAt());
        member.setUpdatedBy(po.getUpdatedBy());
        return member;
    }
    
    /**
     * Domain 转 PO
     */
    public static MemberPO toPO(Member member) {
        if (member == null) {
            return null;
        }
        
        MemberPO po = new MemberPO();
        po.setId(member.getId());
        po.setTenantId(member.getTenantId());
        po.setUserId(member.getUserId());
        po.setMemberNo(member.getMemberNo());
        po.setStatus(member.getStatus().name());
        po.setCreatedAt(member.getCreatedAt());
        po.setCreatedBy(member.getCreatedBy());
        po.setUpdatedAt(member.getUpdatedAt());
        po.setUpdatedBy(member.getUpdatedBy());
        return po;
    }
}
