package com.bluecone.app.growth.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.growth.domain.model.RewardIssueLog;
import com.bluecone.app.growth.domain.repository.RewardIssueLogRepository;
import com.bluecone.app.growth.infrastructure.converter.GrowthConverter;
import com.bluecone.app.growth.infrastructure.persistence.mapper.RewardIssueLogMapper;
import com.bluecone.app.growth.infrastructure.persistence.po.RewardIssueLogPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 奖励发放日志仓储实现
 */
@Repository
@RequiredArgsConstructor
public class RewardIssueLogRepositoryImpl implements RewardIssueLogRepository {
    
    private final RewardIssueLogMapper mapper;
    
    @Override
    public void save(RewardIssueLog log) {
        RewardIssueLogPO po = GrowthConverter.toPO(log);
        mapper.insert(po);
    }
    
    @Override
    public void update(RewardIssueLog log) {
        RewardIssueLogPO po = GrowthConverter.toPO(log);
        mapper.updateById(po);
    }
    
    @Override
    public Optional<RewardIssueLog> findByIdempotencyKey(Long tenantId, String idempotencyKey) {
        LambdaQueryWrapper<RewardIssueLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RewardIssueLogPO::getTenantId, tenantId)
               .eq(RewardIssueLogPO::getIdempotencyKey, idempotencyKey);
        RewardIssueLogPO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(GrowthConverter.toDomain(po));
    }
    
    @Override
    public Optional<RewardIssueLog> findByAttributionAndRole(Long tenantId, Long attributionId, String userRole) {
        LambdaQueryWrapper<RewardIssueLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RewardIssueLogPO::getTenantId, tenantId)
               .eq(RewardIssueLogPO::getAttributionId, attributionId)
               .eq(RewardIssueLogPO::getUserRole, userRole);
        RewardIssueLogPO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(GrowthConverter.toDomain(po));
    }
}
