package com.bluecone.app.growth.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.growth.domain.model.Attribution;
import com.bluecone.app.growth.domain.repository.AttributionRepository;
import com.bluecone.app.growth.infrastructure.converter.GrowthConverter;
import com.bluecone.app.growth.infrastructure.persistence.mapper.AttributionMapper;
import com.bluecone.app.growth.infrastructure.persistence.po.AttributionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 归因关系仓储实现
 */
@Repository
@RequiredArgsConstructor
public class AttributionRepositoryImpl implements AttributionRepository {
    
    private final AttributionMapper mapper;
    
    @Override
    public void save(Attribution attribution) {
        AttributionPO po = GrowthConverter.toPO(attribution);
        mapper.insert(po);
    }
    
    @Override
    public void update(Attribution attribution) {
        AttributionPO po = GrowthConverter.toPO(attribution);
        mapper.updateById(po);
    }
    
    @Override
    public Optional<Attribution> findByInvitee(Long tenantId, String campaignCode, Long inviteeUserId) {
        LambdaQueryWrapper<AttributionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttributionPO::getTenantId, tenantId)
               .eq(AttributionPO::getCampaignCode, campaignCode)
               .eq(AttributionPO::getInviteeUserId, inviteeUserId);
        AttributionPO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(GrowthConverter.toDomain(po));
    }
    
    @Override
    public Optional<Attribution> findById(Long id) {
        AttributionPO po = mapper.selectById(id);
        return Optional.ofNullable(GrowthConverter.toDomain(po));
    }
    
    @Override
    public int countPaidOrdersByUser(Long tenantId, Long userId) {
        return mapper.countPaidOrdersByUser(tenantId, userId);
    }
}
