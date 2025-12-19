package com.bluecone.app.member.infra.repository;

import com.bluecone.app.member.domain.model.Member;
import com.bluecone.app.member.domain.repository.MemberRepository;
import com.bluecone.app.member.infra.converter.MemberConverter;
import com.bluecone.app.member.infra.persistence.mapper.MemberMapper;
import com.bluecone.app.member.infra.persistence.po.MemberPO;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 会员仓储实现
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Repository("memberMemberRepositoryImpl")
public class MemberRepositoryImpl implements MemberRepository {
    
    private final MemberMapper memberMapper;
    
    public MemberRepositoryImpl(MemberMapper memberMapper) {
        this.memberMapper = memberMapper;
    }
    
    @Override
    public Optional<Member> findById(Long tenantId, Long memberId) {
        MemberPO po = memberMapper.selectByMemberId(tenantId, memberId);
        return Optional.ofNullable(MemberConverter.toDomain(po));
    }
    
    @Override
    public Optional<Member> findByUserId(Long tenantId, Long userId) {
        MemberPO po = memberMapper.selectByUserId(tenantId, userId);
        return Optional.ofNullable(MemberConverter.toDomain(po));
    }
    
    @Override
    public void save(Member member) {
        MemberPO po = MemberConverter.toPO(member);
        memberMapper.insert(po);
    }
    
    @Override
    public void update(Member member) {
        MemberPO po = MemberConverter.toPO(member);
        memberMapper.updateById(po);
    }
}
