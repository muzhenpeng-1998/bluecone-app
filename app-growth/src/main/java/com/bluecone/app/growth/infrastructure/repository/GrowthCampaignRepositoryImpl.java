package com.bluecone.app.growth.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.growth.domain.model.GrowthCampaign;
import com.bluecone.app.growth.domain.repository.GrowthCampaignRepository;
import com.bluecone.app.growth.infrastructure.converter.GrowthConverter;
import com.bluecone.app.growth.infrastructure.persistence.mapper.GrowthCampaignMapper;
import com.bluecone.app.growth.infrastructure.persistence.po.GrowthCampaignPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 增长活动仓储实现
 */
@Repository
@RequiredArgsConstructor
public class GrowthCampaignRepositoryImpl implements GrowthCampaignRepository {
    
    private final GrowthCampaignMapper mapper;
    
    @Override
    public void save(GrowthCampaign campaign) {
        GrowthCampaignPO po = GrowthConverter.toPO(campaign);
        mapper.insert(po);
    }
    
    @Override
    public void update(GrowthCampaign campaign) {
        GrowthCampaignPO po = GrowthConverter.toPO(campaign);
        mapper.updateById(po);
    }
    
    @Override
    public Optional<GrowthCampaign> findByCode(Long tenantId, String campaignCode) {
        LambdaQueryWrapper<GrowthCampaignPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GrowthCampaignPO::getTenantId, tenantId)
               .eq(GrowthCampaignPO::getCampaignCode, campaignCode);
        GrowthCampaignPO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(GrowthConverter.toDomain(po));
    }
    
    @Override
    public List<GrowthCampaign> findByTenantId(Long tenantId) {
        LambdaQueryWrapper<GrowthCampaignPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GrowthCampaignPO::getTenantId, tenantId)
               .orderByDesc(GrowthCampaignPO::getCreatedAt);
        return mapper.selectList(wrapper).stream()
                .map(GrowthConverter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<GrowthCampaign> findById(Long id) {
        GrowthCampaignPO po = mapper.selectById(id);
        return Optional.ofNullable(GrowthConverter.toDomain(po));
    }
}
