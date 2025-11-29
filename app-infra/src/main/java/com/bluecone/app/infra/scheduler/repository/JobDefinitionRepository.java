package com.bluecone.app.infra.scheduler.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bluecone.app.infra.scheduler.entity.JobDefinitionEntity;
import com.bluecone.app.infra.scheduler.mapper.JobDefinitionMapper;

/**
 * Job 定义持久化封装，隐藏 MyBatis-Plus 细节。
 */
@Repository
public class JobDefinitionRepository {

    private final JobDefinitionMapper mapper;

    public JobDefinitionRepository(JobDefinitionMapper mapper) {
        this.mapper = mapper;
    }

    public JobDefinitionEntity findByCode(String code) {
        LambdaQueryWrapper<JobDefinitionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobDefinitionEntity::getCode, code).last("limit 1");
        return mapper.selectOne(wrapper);
    }

    public void insert(JobDefinitionEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        mapper.insert(entity);
    }

    public List<JobDefinitionEntity> listEnabled() {
        LambdaQueryWrapper<JobDefinitionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobDefinitionEntity::getEnabled, true);
        return mapper.selectList(wrapper);
    }

    public List<JobDefinitionEntity> listAll() {
        return mapper.selectList(new LambdaQueryWrapper<>());
    }

    public void updateNextRun(String code, LocalDateTime nextRunAt) {
        LambdaUpdateWrapper<JobDefinitionEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(JobDefinitionEntity::getCode, code)
                .set(JobDefinitionEntity::getNextRunAt, nextRunAt)
                .set(JobDefinitionEntity::getUpdatedAt, LocalDateTime.now());
        mapper.update(null, wrapper);
    }

    public void updateRunResult(String code,
                                LocalDateTime lastRunAt,
                                LocalDateTime nextRunAt) {
        LambdaUpdateWrapper<JobDefinitionEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(JobDefinitionEntity::getCode, code)
                .set(JobDefinitionEntity::getLastRunAt, lastRunAt)
                .set(JobDefinitionEntity::getNextRunAt, nextRunAt)
                .set(JobDefinitionEntity::getUpdatedAt, LocalDateTime.now());
        mapper.update(null, wrapper);
    }

    public void updateEnabled(String code, boolean enabled) {
        LambdaUpdateWrapper<JobDefinitionEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(JobDefinitionEntity::getCode, code)
                .set(JobDefinitionEntity::getEnabled, enabled)
                .set(JobDefinitionEntity::getUpdatedAt, LocalDateTime.now());
        mapper.update(null, wrapper);
    }
}
