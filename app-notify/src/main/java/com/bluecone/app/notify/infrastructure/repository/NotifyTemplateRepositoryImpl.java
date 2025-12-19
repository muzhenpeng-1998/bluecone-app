package com.bluecone.app.notify.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.notify.api.enums.NotificationChannel;
import com.bluecone.app.notify.domain.model.NotifyTemplate;
import com.bluecone.app.notify.domain.repository.NotifyTemplateRepository;
import com.bluecone.app.notify.infrastructure.converter.NotifyConverter;
import com.bluecone.app.notify.infrastructure.dao.NotifyTemplateDO;
import com.bluecone.app.notify.infrastructure.dao.NotifyTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 通知模板仓储实现
 */
@Repository
@RequiredArgsConstructor
public class NotifyTemplateRepositoryImpl implements NotifyTemplateRepository {
    
    private final NotifyTemplateMapper mapper;
    
    @Override
    public Long save(NotifyTemplate template) {
        NotifyTemplateDO dataObject = NotifyConverter.toDO(template);
        mapper.insert(dataObject);
        return dataObject.getId();
    }
    
    @Override
    public boolean update(NotifyTemplate template) {
        NotifyTemplateDO dataObject = NotifyConverter.toDO(template);
        return mapper.updateById(dataObject) > 0;
    }
    
    @Override
    public boolean delete(Long id) {
        return mapper.deleteById(id) > 0;
    }
    
    @Override
    public Optional<NotifyTemplate> findById(Long id) {
        NotifyTemplateDO dataObject = mapper.selectById(id);
        return Optional.ofNullable(NotifyConverter.toDomain(dataObject));
    }
    
    @Override
    public Optional<NotifyTemplate> findByCodeAndChannel(String templateCode, NotificationChannel channel, Long tenantId) {
        LambdaQueryWrapper<NotifyTemplateDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotifyTemplateDO::getTemplateCode, templateCode)
               .eq(NotifyTemplateDO::getChannel, channel.name())
               .and(w -> w.eq(NotifyTemplateDO::getTenantId, tenantId).or().isNull(NotifyTemplateDO::getTenantId))
               .orderByDesc(NotifyTemplateDO::getTenantId) // 租户级优先
               .last("LIMIT 1");
        NotifyTemplateDO dataObject = mapper.selectOne(wrapper);
        return Optional.ofNullable(NotifyConverter.toDomain(dataObject));
    }
    
    @Override
    public List<NotifyTemplate> findByBizType(String bizType, Long tenantId) {
        LambdaQueryWrapper<NotifyTemplateDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotifyTemplateDO::getBizType, bizType);
        if (tenantId != null) {
            wrapper.and(w -> w.eq(NotifyTemplateDO::getTenantId, tenantId).or().isNull(NotifyTemplateDO::getTenantId));
        }
        return mapper.selectList(wrapper).stream()
                .map(NotifyConverter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<NotifyTemplate> findAll(Long tenantId) {
        LambdaQueryWrapper<NotifyTemplateDO> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            wrapper.and(w -> w.eq(NotifyTemplateDO::getTenantId, tenantId).or().isNull(NotifyTemplateDO::getTenantId));
        }
        return mapper.selectList(wrapper).stream()
                .map(NotifyConverter::toDomain)
                .collect(Collectors.toList());
    }
}
