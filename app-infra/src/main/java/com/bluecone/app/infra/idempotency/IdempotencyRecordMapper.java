package com.bluecone.app.infra.idempotency;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * 幂等记录 Mapper。
 */
@Mapper
public interface IdempotencyRecordMapper extends BaseMapper<IdempotencyRecordDO> {
}

