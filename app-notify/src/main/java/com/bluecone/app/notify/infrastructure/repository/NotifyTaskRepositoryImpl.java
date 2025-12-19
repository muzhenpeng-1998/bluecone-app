package com.bluecone.app.notify.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.notify.api.enums.NotificationTaskStatus;
import com.bluecone.app.notify.domain.model.NotifyTask;
import com.bluecone.app.notify.domain.repository.NotifyTaskRepository;
import com.bluecone.app.notify.infrastructure.converter.NotifyConverter;
import com.bluecone.app.notify.infrastructure.dao.NotifyTaskDO;
import com.bluecone.app.notify.infrastructure.dao.NotifyTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 通知任务仓储实现
 */
@Repository
@RequiredArgsConstructor
public class NotifyTaskRepositoryImpl implements NotifyTaskRepository {
    
    private final NotifyTaskMapper mapper;
    
    @Override
    public Long save(NotifyTask task) {
        NotifyTaskDO dataObject = NotifyConverter.toDO(task);
        mapper.insert(dataObject);
        return dataObject.getId();
    }
    
    @Override
    public boolean update(NotifyTask task) {
        NotifyTaskDO dataObject = NotifyConverter.toDO(task);
        return mapper.updateById(dataObject) > 0;
    }
    
    @Override
    public Optional<NotifyTask> findById(Long id) {
        NotifyTaskDO dataObject = mapper.selectById(id);
        return Optional.ofNullable(NotifyConverter.toDomain(dataObject));
    }
    
    @Override
    public Optional<NotifyTask> findByIdempotencyKey(String idempotencyKey) {
        LambdaQueryWrapper<NotifyTaskDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotifyTaskDO::getIdempotencyKey, idempotencyKey);
        NotifyTaskDO dataObject = mapper.selectOne(wrapper);
        return Optional.ofNullable(NotifyConverter.toDomain(dataObject));
    }
    
    @Override
    public List<NotifyTask> findPendingTasks(int limit) {
        return mapper.selectPendingTasks(limit).stream()
                .map(NotifyConverter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<NotifyTask> findTasksForRetry(int limit) {
        return mapper.selectTasksForRetry(LocalDateTime.now(), limit).stream()
                .map(NotifyConverter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<NotifyTask> findByUserAndBizType(Long userId, String bizType, int limit) {
        LambdaQueryWrapper<NotifyTaskDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotifyTaskDO::getUserId, userId)
               .eq(NotifyTaskDO::getBizType, bizType)
               .orderByDesc(NotifyTaskDO::getCreatedAt)
               .last("LIMIT " + limit);
        return mapper.selectList(wrapper).stream()
                .map(NotifyConverter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public int countByStatus(NotificationTaskStatus status) {
        LambdaQueryWrapper<NotifyTaskDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotifyTaskDO::getStatus, status.name());
        return mapper.selectCount(wrapper).intValue();
    }
}
