package com.bluecone.app.growth.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.growth.domain.model.InviteCode;
import com.bluecone.app.growth.domain.repository.InviteCodeRepository;
import com.bluecone.app.growth.infrastructure.converter.GrowthConverter;
import com.bluecone.app.growth.infrastructure.persistence.mapper.InviteCodeMapper;
import com.bluecone.app.growth.infrastructure.persistence.po.InviteCodePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 邀请码仓储实现
 */
@Repository
@RequiredArgsConstructor
public class InviteCodeRepositoryImpl implements InviteCodeRepository {
    
    private final InviteCodeMapper mapper;
    
    @Override
    public void save(InviteCode inviteCode) {
        InviteCodePO po = GrowthConverter.toPO(inviteCode);
        mapper.insert(po);
    }
    
    @Override
    public void update(InviteCode inviteCode) {
        InviteCodePO po = GrowthConverter.toPO(inviteCode);
        mapper.updateById(po);
    }
    
    @Override
    public Optional<InviteCode> findByInviteCode(String inviteCode) {
        LambdaQueryWrapper<InviteCodePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InviteCodePO::getInviteCode, inviteCode);
        InviteCodePO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(GrowthConverter.toDomain(po));
    }
    
    @Override
    public Optional<InviteCode> findByInviter(Long tenantId, String campaignCode, Long inviterUserId) {
        LambdaQueryWrapper<InviteCodePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InviteCodePO::getTenantId, tenantId)
               .eq(InviteCodePO::getCampaignCode, campaignCode)
               .eq(InviteCodePO::getInviterUserId, inviterUserId);
        InviteCodePO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(GrowthConverter.toDomain(po));
    }
    
    @Override
    public void incrementInvitesCount(Long id) {
        mapper.incrementInvitesCount(id);
    }
    
    @Override
    public void incrementSuccessfulInvitesCount(Long id) {
        mapper.incrementSuccessfulInvitesCount(id);
    }
}
