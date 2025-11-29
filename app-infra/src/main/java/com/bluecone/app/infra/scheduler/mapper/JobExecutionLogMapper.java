package com.bluecone.app.infra.scheduler.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.scheduler.entity.JobExecutionLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobExecutionLogMapper extends BaseMapper<JobExecutionLogEntity> {
}
