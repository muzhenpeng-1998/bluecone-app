package com.bluecone.app.infra.idempotency;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bluecone.app.core.idempotency.domain.IdemStatus;
import com.bluecone.app.core.idempotency.domain.IdempotencyRecord;
import com.bluecone.app.core.idempotency.spi.IdempotencyRepository;

/**
 * 基于 MyBatis-Plus + MySQL 的幂等存储实现。
 */
@Repository
public class IdempotencyRepositoryImpl implements IdempotencyRepository {

    private final IdempotencyRecordMapper mapper;
    private final Clock clock;

    @Autowired
    public IdempotencyRepositoryImpl(IdempotencyRecordMapper mapper) {
        this(mapper, Clock.systemUTC());
    }

    public IdempotencyRepositoryImpl(IdempotencyRecordMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public Optional<IdempotencyRecord> find(long tenantId, String bizType, String idemKey) {
        LambdaQueryWrapper<IdempotencyRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IdempotencyRecordDO::getTenantId, tenantId)
                .eq(IdempotencyRecordDO::getBizType, bizType)
                .eq(IdempotencyRecordDO::getIdemKey, idemKey);
        IdempotencyRecordDO record = mapper.selectOne(wrapper);
        return Optional.ofNullable(record).map(this::toDomain);
    }

    @Override
    public AcquireResult tryAcquire(AcquireCommand command) {
        Instant now = Instant.now(clock);
        LocalDateTime nowDt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);

        IdempotencyRecordDO insert = new IdempotencyRecordDO();
        insert.setTenantId(command.tenantId());
        insert.setBizType(command.bizType());
        insert.setIdemKey(command.idemKey());
        insert.setRequestHash(command.requestHash());
        insert.setStatus(0); // PROCESSING
        insert.setExpireAt(LocalDateTime.ofInstant(command.expireAt(), ZoneOffset.UTC));
        insert.setLockUntil(LocalDateTime.ofInstant(command.lockUntil(), ZoneOffset.UTC));
        insert.setVersion(0);
        insert.setCreatedAt(nowDt);
        insert.setUpdatedAt(nowDt);

        try {
            mapper.insert(insert);
            return new AcquireResult(AcquireState.ACQUIRED, toDomain(insert));
        } catch (DuplicateKeyException ex) {
            // 记录已存在，按状态处理
            LambdaQueryWrapper<IdempotencyRecordDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(IdempotencyRecordDO::getTenantId, command.tenantId())
                    .eq(IdempotencyRecordDO::getBizType, command.bizType())
                    .eq(IdempotencyRecordDO::getIdemKey, command.idemKey());
            IdempotencyRecordDO existing = mapper.selectOne(wrapper);
            if (existing == null) {
                // 并发删除后重试
                return new AcquireResult(AcquireState.RETRYABLE, null);
            }
            IdemStatus status = fromInt(existing.getStatus());
            LocalDateTime expireAt = existing.getExpireAt();
            LocalDateTime lockUntil = existing.getLockUntil();

            // 请求摘要不同 => 冲突
            if (!command.requestHash().equals(existing.getRequestHash())) {
                return new AcquireResult(AcquireState.CONFLICT, toDomain(existing));
            }

            // 已成功且未过期 => 重放
            if (status == IdemStatus.SUCCEEDED && expireAt != null && expireAt.isAfter(nowDt)) {
                return new AcquireResult(AcquireState.REPLAY_SUCCEEDED, toDomain(existing));
            }

            // 正在处理
            if (status == IdemStatus.PROCESSING) {
                if (lockUntil != null && lockUntil.isAfter(nowDt)) {
                    return new AcquireResult(AcquireState.IN_PROGRESS, toDomain(existing));
                }
                // 租约过期，尝试 DB 抢占
                LambdaUpdateWrapper<IdempotencyRecordDO> update = new LambdaUpdateWrapper<>();
                update.eq(IdempotencyRecordDO::getTenantId, command.tenantId())
                        .eq(IdempotencyRecordDO::getBizType, command.bizType())
                        .eq(IdempotencyRecordDO::getIdemKey, command.idemKey())
                        .le(IdempotencyRecordDO::getLockUntil, nowDt);
                IdempotencyRecordDO toUpdate = new IdempotencyRecordDO();
                toUpdate.setStatus(0);
                toUpdate.setLockUntil(LocalDateTime.ofInstant(command.lockUntil(), ZoneOffset.UTC));
                toUpdate.setExpireAt(LocalDateTime.ofInstant(command.expireAt(), ZoneOffset.UTC));
                toUpdate.setUpdatedAt(nowDt);
                int rows = mapper.update(toUpdate, update);
                if (rows > 0) {
                    IdempotencyRecordDO latest = mapper.selectOne(wrapper);
                    return new AcquireResult(AcquireState.ACQUIRED, toDomain(latest));
                }
                return new AcquireResult(AcquireState.IN_PROGRESS, toDomain(existing));
            }

            // 失败记录：当前实现中视为仍由上游决定是否重试，此处标记为 IN_PROGRESS
            if (status == IdemStatus.FAILED && expireAt != null && expireAt.isAfter(nowDt)) {
                return new AcquireResult(AcquireState.IN_PROGRESS, toDomain(existing));
            }

            // 过期记录：尝试重置为 PROCESSING
            if (expireAt != null && !expireAt.isAfter(nowDt)) {
                LambdaUpdateWrapper<IdempotencyRecordDO> update = new LambdaUpdateWrapper<>();
                update.eq(IdempotencyRecordDO::getTenantId, command.tenantId())
                        .eq(IdempotencyRecordDO::getBizType, command.bizType())
                        .eq(IdempotencyRecordDO::getIdemKey, command.idemKey())
                        .le(IdempotencyRecordDO::getExpireAt, nowDt);
                IdempotencyRecordDO toUpdate = new IdempotencyRecordDO();
                toUpdate.setStatus(0);
                toUpdate.setRequestHash(command.requestHash());
                toUpdate.setLockUntil(LocalDateTime.ofInstant(command.lockUntil(), ZoneOffset.UTC));
                toUpdate.setExpireAt(LocalDateTime.ofInstant(command.expireAt(), ZoneOffset.UTC));
                toUpdate.setUpdatedAt(nowDt);
                int rows = mapper.update(toUpdate, update);
                if (rows > 0) {
                    IdempotencyRecordDO latest = mapper.selectOne(wrapper);
                    return new AcquireResult(AcquireState.ACQUIRED, toDomain(latest));
                }
            }

            return new AcquireResult(AcquireState.IN_PROGRESS, toDomain(existing));
        }
    }

    @Override
    public void markSuccess(MarkSuccessCommand command) {
        Instant now = Instant.now(clock);
        LocalDateTime nowDt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        LambdaUpdateWrapper<IdempotencyRecordDO> update = new LambdaUpdateWrapper<>();
        update.eq(IdempotencyRecordDO::getTenantId, command.tenantId())
                .eq(IdempotencyRecordDO::getBizType, command.bizType())
                .eq(IdempotencyRecordDO::getIdemKey, command.idemKey())
                .eq(IdempotencyRecordDO::getRequestHash, command.requestHash());
        IdempotencyRecordDO toUpdate = new IdempotencyRecordDO();
        toUpdate.setStatus(1); // SUCCEEDED
        toUpdate.setResultRef(command.resultRef());
        toUpdate.setResultJson(command.resultJson());
        toUpdate.setExpireAt(LocalDateTime.ofInstant(command.expireAt(), ZoneOffset.UTC));
        toUpdate.setUpdatedAt(nowDt);
        mapper.update(toUpdate, update);
    }

    @Override
    public void markFailed(MarkFailedCommand command) {
        Instant now = Instant.now(clock);
        LocalDateTime nowDt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        LambdaUpdateWrapper<IdempotencyRecordDO> update = new LambdaUpdateWrapper<>();
        update.eq(IdempotencyRecordDO::getTenantId, command.tenantId())
                .eq(IdempotencyRecordDO::getBizType, command.bizType())
                .eq(IdempotencyRecordDO::getIdemKey, command.idemKey())
                .eq(IdempotencyRecordDO::getRequestHash, command.requestHash());
        IdempotencyRecordDO toUpdate = new IdempotencyRecordDO();
        toUpdate.setStatus(2); // FAILED
        toUpdate.setErrorCode(command.errorCode());
        toUpdate.setErrorMsg(command.errorMsg());
        toUpdate.setExpireAt(LocalDateTime.ofInstant(command.expireAt(), ZoneOffset.UTC));
        toUpdate.setUpdatedAt(nowDt);
        mapper.update(toUpdate, update);
    }

    private IdemStatus fromInt(Integer status) {
        if (status == null) {
            return IdemStatus.PROCESSING;
        }
        return switch (status) {
            case 0 -> IdemStatus.PROCESSING;
            case 1 -> IdemStatus.SUCCEEDED;
            case 2 -> IdemStatus.FAILED;
            default -> IdemStatus.PROCESSING;
        };
    }

    private IdempotencyRecord toDomain(IdempotencyRecordDO record) {
        return new IdempotencyRecord(
                record.getId(),
                record.getTenantId(),
                record.getBizType(),
                record.getIdemKey(),
                record.getRequestHash(),
                fromInt(record.getStatus()),
                record.getResultRef(),
                record.getResultJson(),
                record.getErrorCode(),
                record.getErrorMsg(),
                toInstant(record.getExpireAt()),
                toInstant(record.getLockUntil()),
                record.getVersion() != null ? record.getVersion() : 0,
                toInstant(record.getCreatedAt()),
                toInstant(record.getUpdatedAt())
        );
    }

    private Instant toInstant(LocalDateTime time) {
        return time == null ? null : time.toInstant(ZoneOffset.UTC);
    }
}
