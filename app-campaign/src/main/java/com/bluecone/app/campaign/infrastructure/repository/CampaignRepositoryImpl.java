package com.bluecone.app.campaign.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.campaign.api.enums.CampaignStatus;
import com.bluecone.app.campaign.api.enums.CampaignType;
import com.bluecone.app.campaign.domain.model.Campaign;
import com.bluecone.app.campaign.domain.repository.CampaignRepository;
import com.bluecone.app.campaign.infrastructure.converter.CampaignConverter;
import com.bluecone.app.campaign.infrastructure.persistence.mapper.CampaignMapper;
import com.bluecone.app.campaign.infrastructure.persistence.po.CampaignPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 活动仓储实现
 */
@Repository
@RequiredArgsConstructor
public class CampaignRepositoryImpl implements CampaignRepository {
    
    private final CampaignMapper campaignMapper;
    private final CampaignConverter converter;
    
    @Override
    public void save(Campaign campaign) {
        CampaignPO po = converter.toPO(campaign);
        campaignMapper.insert(po);
    }
    
    @Override
    public void update(Campaign campaign) {
        CampaignPO po = converter.toPO(campaign);
        campaignMapper.updateById(po);
    }
    
    @Override
    public Optional<Campaign> findById(Long tenantId, Long campaignId) {
        LambdaQueryWrapper<CampaignPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CampaignPO::getTenantId, tenantId)
                .eq(CampaignPO::getId, campaignId);
        
        CampaignPO po = campaignMapper.selectOne(wrapper);
        return Optional.ofNullable(converter.toDomain(po));
    }
    
    @Override
    public Optional<Campaign> findByCode(Long tenantId, String campaignCode) {
        LambdaQueryWrapper<CampaignPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CampaignPO::getTenantId, tenantId)
                .eq(CampaignPO::getCampaignCode, campaignCode);
        
        CampaignPO po = campaignMapper.selectOne(wrapper);
        return Optional.ofNullable(converter.toDomain(po));
    }
    
    @Override
    public List<Campaign> findByTenant(Long tenantId, CampaignType campaignType) {
        LambdaQueryWrapper<CampaignPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CampaignPO::getTenantId, tenantId);
        
        if (campaignType != null) {
            wrapper.eq(CampaignPO::getCampaignType, campaignType.name());
        }
        
        wrapper.orderByDesc(CampaignPO::getCreatedAt);
        
        List<CampaignPO> pos = campaignMapper.selectList(wrapper);
        return pos.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Campaign> findAvailableCampaigns(Long tenantId, CampaignType campaignType,
                                                  CampaignStatus status, LocalDateTime queryTime) {
        List<CampaignPO> pos = campaignMapper.selectAvailableCampaigns(
                tenantId, 
                campaignType.name(), 
                status.name(), 
                queryTime
        );
        
        return pos.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public void delete(Long tenantId, Long campaignId) {
        LambdaQueryWrapper<CampaignPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CampaignPO::getTenantId, tenantId)
                .eq(CampaignPO::getId, campaignId);
        
        campaignMapper.delete(wrapper);
    }
}
