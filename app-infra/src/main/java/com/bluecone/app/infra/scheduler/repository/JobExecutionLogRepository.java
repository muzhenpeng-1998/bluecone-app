package com.bluecone.app.infra.scheduler.repository;

import org.springframework.stereotype.Repository;

import com.bluecone.app.infra.scheduler.entity.JobExecutionLogEntity;
import com.bluecone.app.infra.scheduler.mapper.JobExecutionLogMapper;

/**
 * 任务执行日志持久化。
 */
@Repository
public class JobExecutionLogRepository {

    private final JobExecutionLogMapper mapper;

    public JobExecutionLogRepository(JobExecutionLogMapper mapper) {
        this.mapper = mapper;
    }

    public void save(JobExecutionLogEntity entity) {
        mapper.insert(entity);
    }
}
