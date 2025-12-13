package com.bluecone.app.infra.event.consume;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bluecone.app.core.event.consume.spi.EventDedupRepository;
import com.bluecone.app.id.core.Ulid128;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * 基于 MyBatis-Plus + MySQL 的事件消费去重实现。
 */
@Repository
public class MysqlEventDedupRepository implements EventDedupRepository {

    private static final int STATUS_PROCESSING = 0;
    private static final int STATUS_SUCCEEDED = 1;
    private static final int STATUS_FAILED = 2;

    private final EventConsumeRecordMapper mapper;
    private final Clock clock;

    @Autowired
    public MysqlEventDedupRepository(EventConsumeRecordMapper mapper) {
        this(mapper, Clock.systemUTC());
    }

    public MysqlEventDedupRepository(EventConsumeRecordMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public AcquireConsumeResult tryAcquire(AcquireConsumeCommand command) {
        Instant now = Instant.now(clock);
        LocalDateTime nowDt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        LocalDateTime lockUntilDt = LocalDateTime.ofInstant(command.lockedUntil(), ZoneOffset.UTC);

        EventConsumeRecordDO insert = new EventConsumeRecordDO();
        insert.setTenantId(command.tenantId());
        insert.setConsumerGroup(command.consumerGroup());
        insert.setEventId(command.eventId());
        insert.setEventType(command.eventType());
        insert.setStatus(STATUS_PROCESSING);
        insert.setLockedBy("");
        insert.setLockedUntil(lockUntilDt);
        insert.setNextRetryAt(nowDt);
        insert.setRetryCount(0);
        insert.setCreatedAt(nowDt);
        insert.setUpdatedAt(nowDt);

        try {
            mapper.insert(insert);
            return new AcquireConsumeResult(AcquireConsumeState.ACQUIRED, toDomain(insert));
        } catch (DuplicateKeyException ex) {
            LambdaQueryWrapper<EventConsumeRecordDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(EventConsumeRecordDO::getConsumerGroup, command.consumerGroup())
                    .eq(EventConsumeRecordDO::getEventId, command.eventId());
            EventConsumeRecordDO existing = mapper.selectOne(wrapper);
            if (existing == null) {
                return new AcquireConsumeResult(AcquireConsumeState.RETRYABLE_FAILED, null);
            }

            if (!command.eventType().equals(existing.getEventType())) {
                return new AcquireConsumeResult(AcquireConsumeState.CONFLICT, toDomain(existing));
            }

            int status = existing.getStatus() == null ? STATUS_PROCESSING : existing.getStatus();
            LocalDateTime lockedUntil = existing.getLockedUntil();
            LocalDateTime nextRetryAt = existing.getNextRetryAt();

            if (status == STATUS_SUCCEEDED) {
                return new AcquireConsumeResult(AcquireConsumeState.REPLAY_SUCCEEDED, toDomain(existing));
            }

            if (status == STATUS_PROCESSING) {
                if (lockedUntil != null && lockedUntil.isAfter(nowDt)) {
                    return new AcquireConsumeResult(AcquireConsumeState.IN_PROGRESS, toDomain(existing));
                }
                LambdaUpdateWrapper<EventConsumeRecordDO> update = new LambdaUpdateWrapper<>();
                update.eq(EventConsumeRecordDO::getConsumerGroup, command.consumerGroup())
                        .eq(EventConsumeRecordDO::getEventId, command.eventId())
                        .le(EventConsumeRecordDO::getLockedUntil, nowDt);
                EventConsumeRecordDO toUpdate = new EventConsumeRecordDO();
                toUpdate.setStatus(STATUS_PROCESSING);
                toUpdate.setLockedUntil(lockUntilDt);
                toUpdate.setUpdatedAt(nowDt);
                int rows = mapper.update(toUpdate, update);
                if (rows > 0) {
                    EventConsumeRecordDO latest = mapper.selectOne(wrapper);
                    return new AcquireConsumeResult(AcquireConsumeState.ACQUIRED, toDomain(latest));
                }
                return new AcquireConsumeResult(AcquireConsumeState.IN_PROGRESS, toDomain(existing));
            }

            if (status == STATUS_FAILED) {
                if (nextRetryAt != null && nextRetryAt.isAfter(nowDt)) {
                    return new AcquireConsumeResult(AcquireConsumeState.IN_PROGRESS, toDomain(existing));
                }
                if (lockedUntil != null && lockedUntil.isAfter(nowDt)) {
                    return new AcquireConsumeResult(AcquireConsumeState.IN_PROGRESS, toDomain(existing));
                }
                LambdaUpdateWrapper<EventConsumeRecordDO> update = new LambdaUpdateWrapper<>();
                update.eq(EventConsumeRecordDO::getConsumerGroup, command.consumerGroup())
                        .eq(EventConsumeRecordDO::getEventId, command.eventId())
                        .le(EventConsumeRecordDO::getLockedUntil, nowDt);
                EventConsumeRecordDO toUpdate = new EventConsumeRecordDO();
                toUpdate.setStatus(STATUS_PROCESSING);
                toUpdate.setLockedUntil(lockUntilDt);
                toUpdate.setUpdatedAt(nowDt);
                int rows = mapper.update(toUpdate, update);
                if (rows > 0) {
                    EventConsumeRecordDO latest = mapper.selectOne(wrapper);
                    return new AcquireConsumeResult(AcquireConsumeState.RETRYABLE_FAILED, toDomain(latest));
                }
                return new AcquireConsumeResult(AcquireConsumeState.IN_PROGRESS, toDomain(existing));
            }

            return new AcquireConsumeResult(AcquireConsumeState.IN_PROGRESS, toDomain(existing));
        }
    }

    @Override
    public Optional<ConsumeRecord> find(String consumerGroup, Ulid128 eventId) {
        LambdaQueryWrapper<EventConsumeRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventConsumeRecordDO::getConsumerGroup, consumerGroup)
                .eq(EventConsumeRecordDO::getEventId, eventId);
        EventConsumeRecordDO record = mapper.selectOne(wrapper);
        return Optional.ofNullable(record).map(this::toDomain);
    }

    @Override
    public void markSuccess(MarkConsumeSuccessCommand command) {
        Instant now = Instant.now(clock);
        LocalDateTime nowDt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        LambdaUpdateWrapper<EventConsumeRecordDO> update = new LambdaUpdateWrapper<>();
        update.eq(EventConsumeRecordDO::getConsumerGroup, command.consumerGroup())
                .eq(EventConsumeRecordDO::getEventId, command.eventId())
                .eq(EventConsumeRecordDO::getStatus, STATUS_PROCESSING);
        EventConsumeRecordDO toUpdate = new EventConsumeRecordDO();
        toUpdate.setStatus(STATUS_SUCCEEDED);
        toUpdate.setProcessedAt(LocalDateTime.ofInstant(command.processedAt(), ZoneOffset.UTC));
        toUpdate.setUpdatedAt(nowDt);
        mapper.update(toUpdate, update);
    }

    @Override
    public void markFailed(MarkConsumeFailedCommand command) {
        Instant now = Instant.now(clock);
        LocalDateTime nowDt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        LambdaUpdateWrapper<EventConsumeRecordDO> update = new LambdaUpdateWrapper<>();
        update.eq(EventConsumeRecordDO::getConsumerGroup, command.consumerGroup())
                .eq(EventConsumeRecordDO::getEventId, command.eventId())
                .eq(EventConsumeRecordDO::getStatus, STATUS_PROCESSING);
        EventConsumeRecordDO toUpdate = new EventConsumeRecordDO();
        toUpdate.setStatus(STATUS_FAILED);
        String rawMsg = command.errorMsg();
        if (rawMsg != null && rawMsg.length() > 256) {
            rawMsg = rawMsg.substring(0, 256);
        }
        toUpdate.setErrorMsg(rawMsg);
        toUpdate.setRetryCount(command.retryCount());
        toUpdate.setNextRetryAt(LocalDateTime.ofInstant(command.nextRetryAt(), ZoneOffset.UTC));
        toUpdate.setUpdatedAt(nowDt);
        mapper.update(toUpdate, update);
    }

    private ConsumeRecord toDomain(EventConsumeRecordDO record) {
        return new ConsumeRecord(
                record.getTenantId(),
                record.getConsumerGroup(),
                record.getEventId(),
                record.getEventType(),
                record.getStatus() == null ? STATUS_PROCESSING : record.getStatus(),
                toInstant(record.getLockedUntil()),
                toInstant(record.getNextRetryAt()),
                record.getRetryCount() == null ? 0 : record.getRetryCount(),
                record.getErrorMsg()
        );
    }

    private Instant toInstant(LocalDateTime time) {
        return time == null ? null : time.toInstant(ZoneOffset.UTC);
    }
}
