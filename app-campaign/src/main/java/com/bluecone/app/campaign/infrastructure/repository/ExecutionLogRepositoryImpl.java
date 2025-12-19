package com.bluecone.app.campaign.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.campaign.domain.model.ExecutionLog;
import com.bluecone.app.campaign.domain.repository.ExecutionLogRepository;
import com.bluecone.app.campaign.infrastructure.converter.CampaignConverter;
import com.bluecone.app.campaign.infrastructure.persistence.mapper.ExecutionLogMapper;
import com.bluecone.app.campaign.infrastructure.persistence.po.ExecutionLogPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 活动执行日志仓储实现
 */
@Repository
@RequiredArgsConstructor
public class ExecutionLogRepositoryImpl implements ExecutionLogRepository {
    
    private final ExecutionLogMapper executionLogMapper;
    private final CampaignConverter converter;
    
    @Override
    public void save(ExecutionLog log) {
        ExecutionLogPO po = converter.toExecutionLogPO(log);
        executionLogMapper.insert(po);
    }
    
    @Override
    public void update(ExecutionLog log) {
        ExecutionLogPO po = converter.toExecutionLogPO(log);
        executionLogMapper.updateById(po);
    }
    
    @Override
    public Optional<ExecutionLog> findByIdempotencyKey(Long tenantId, String idempotencyKey) {
        LambdaQueryWrapper<ExecutionLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExecutionLogPO::getTenantId, tenantId)
                .eq(ExecutionLogPO::getIdempotencyKey, idempotencyKey);
        
        ExecutionLogPO po = executionLogMapper.selectOne(wrapper);
        return Optional.ofNullable(converter.toExecutionLogDomain(po));
    }
    
    @Override
    public int countUserExecutions(Long tenantId, Long campaignId, Long userId) {
        return executionLogMapper.countUserSuccessExecutions(tenantId, campaignId, userId);
    }
    
    @Override
    public List<ExecutionLog> findByConditions(Long tenantId, Long campaignId, Long userId, Integer limit) {
        LambdaQueryWrapper<ExecutionLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExecutionLogPO::getTenantId, tenantId);
        
        if (campaignId != null) {
            wrapper.eq(ExecutionLogPO::getCampaignId, campaignId);
        }
        
        if (userId != null) {
            wrapper.eq(ExecutionLogPO::getUserId, userId);
        }
        
        wrapper.orderByDesc(ExecutionLogPO::getCreatedAt);
        
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        
        List<ExecutionLogPO> pos = executionLogMapper.selectList(wrapper);
        return pos.stream()
                .map(converter::toExecutionLogDomain)
                .collect(Collectors.toList());
    }
}
