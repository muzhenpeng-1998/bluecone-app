package com.bluecone.app.member.application.service;

import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.member.domain.enums.MemberStatus;
import com.bluecone.app.member.domain.model.Member;
import com.bluecone.app.member.domain.model.PointsAccount;
import com.bluecone.app.member.domain.repository.MemberRepository;
import com.bluecone.app.member.domain.repository.PointsAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 会员应用服务
 * 负责会员的创建、查询等业务逻辑
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Service("pointsMemberApplicationService")
public class MemberApplicationService {
    
    private static final Logger log = LoggerFactory.getLogger(MemberApplicationService.class);
    
    private final MemberRepository memberRepository;
    private final PointsAccountRepository accountRepository;
    private final IdService idService;
    
    public MemberApplicationService(MemberRepository memberRepository,
                                   PointsAccountRepository accountRepository,
                                   IdService idService) {
        this.memberRepository = memberRepository;
        this.accountRepository = accountRepository;
        this.idService = idService;
    }
    
    /**
     * 获取或创建会员（幂等）
     * 如果会员不存在，则自动创建；如果已存在，则返回现有会员
     * 创建会员时，同时创建积分账户
     * 
     * @param tenantId 租户ID
     * @param userId 平台用户ID
     * @return 会员信息
     */
    @Transactional(rollbackFor = Exception.class)
    public Member getOrCreateMember(Long tenantId, Long userId) {
        // 1. 先查询是否已存在
        Optional<Member> existing = memberRepository.findByUserId(tenantId, userId);
        if (existing.isPresent()) {
            log.info("会员已存在，租户ID：{}，用户ID：{}，会员ID：{}", tenantId, userId, existing.get().getId());
            return existing.get();
        }
        
        // 2. 创建新会员
        Long memberId = idService.nextLong(IdScope.MEMBER);
        String memberNo = generateMemberNo(memberId);
        
        Member member = Member.create(memberId, tenantId, userId, memberNo);
        memberRepository.save(member);
        
        log.info("创建新会员成功，租户ID：{}，用户ID：{}，会员ID：{}，会员号：{}", 
                tenantId, userId, memberId, memberNo);
        
        // 3. 创建积分账户
        createPointsAccount(tenantId, memberId);
        
        return member;
    }
    
    /**
     * 根据会员ID查询会员
     * 
     * @param tenantId 租户ID
     * @param memberId 会员ID
     * @return 会员信息，不存在则返回 null
     */
    public Member getMemberById(Long tenantId, Long memberId) {
        return memberRepository.findById(tenantId, memberId).orElse(null);
    }
    
    /**
     * 根据用户ID查询会员
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 会员信息，不存在则返回 null
     */
    public Member getMemberByUserId(Long tenantId, Long userId) {
        return memberRepository.findByUserId(tenantId, userId).orElse(null);
    }
    
    /**
     * 创建积分账户
     */
    private void createPointsAccount(Long tenantId, Long memberId) {
        // 检查是否已存在积分账户
        Optional<PointsAccount> existing = accountRepository.findByMemberId(tenantId, memberId);
        if (existing.isPresent()) {
            log.warn("积分账户已存在，租户ID：{}，会员ID：{}", tenantId, memberId);
            return;
        }
        
        Long accountId = idService.nextLong(IdScope.POINTS_ACCOUNT);
        PointsAccount account = PointsAccount.create(accountId, tenantId, memberId);
        accountRepository.save(account);
        
        log.info("创建积分账户成功，租户ID：{}，会员ID：{}，账户ID：{}", tenantId, memberId, accountId);
    }
    
    /**
     * 生成会员编号
     * 格式：mb_ + 会员ID
     */
    private String generateMemberNo(Long memberId) {
        return "mb_" + memberId;
    }
}
