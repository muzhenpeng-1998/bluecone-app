package com.bluecone.app.infra.idempotency;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Read-only ops queries for idempotency conflicts.
 */
@Repository
public class IdempotencyRecordOpsRepository {

    private final IdempotencyRecordMapper mapper;

    public IdempotencyRecordOpsRepository(final IdempotencyRecordMapper mapper) {
        this.mapper = mapper;
    }

    public List<IdempotencyRecordDO> listConflicts(Long beforeId, int limit) {
        LambdaQueryWrapper<IdempotencyRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(
                        IdempotencyRecordDO::getId,
                        IdempotencyRecordDO::getTenantId,
                        IdempotencyRecordDO::getBizType,
                        IdempotencyRecordDO::getIdemKey,
                        IdempotencyRecordDO::getRequestHash,
                        IdempotencyRecordDO::getStatus,
                        IdempotencyRecordDO::getErrorCode,
                        IdempotencyRecordDO::getErrorMsg,
                        IdempotencyRecordDO::getCreatedAt
                )
                // Treat FAILED records as conflicts for drill-down.
                .eq(IdempotencyRecordDO::getStatus, 2);

        if (beforeId != null) {
            wrapper.lt(IdempotencyRecordDO::getId, beforeId);
        }
        wrapper.orderByDesc(IdempotencyRecordDO::getId)
                .last("limit " + limit);
        return mapper.selectList(wrapper);
    }
}

