package com.bluecone.app.ops.service;

import com.bluecone.app.infra.event.consume.EventConsumeRecordDO;
import com.bluecone.app.infra.event.consume.EventConsumeRecordOpsRepository;
import com.bluecone.app.infra.idempotency.IdempotencyRecordDO;
import com.bluecone.app.infra.idempotency.IdempotencyRecordOpsRepository;
import com.bluecone.app.infra.outbox.entity.OutboxMessageEntity;
import com.bluecone.app.infra.outbox.entity.OutboxMessageStatus;
import com.bluecone.app.infra.outbox.repository.OutboxMessageRepository;
import com.bluecone.app.ops.api.dto.drill.ConsumeItem;
import com.bluecone.app.ops.api.dto.drill.IdemConflictItem;
import com.bluecone.app.ops.api.dto.drill.OutboxItem;
import com.bluecone.app.ops.api.dto.drill.PageResult;
import com.bluecone.app.ops.config.BlueconeOpsProperties;
import com.bluecone.app.id.core.Ulid128;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class DefaultOpsDrillService implements OpsDrillService {

    private static final int CONSUME_STATUS_PROCESSING = 0;
    private static final int CONSUME_STATUS_SUCCEEDED = 1;
    private static final int CONSUME_STATUS_FAILED = 2;

    private final BlueconeOpsProperties properties;
    private final OutboxMessageRepository outboxMessageRepository;
    private final EventConsumeRecordOpsRepository consumeOpsRepository;
    private final IdempotencyRecordOpsRepository idemOpsRepository;
    private final Clock clock;

    private final ConcurrentMap<String, CachedPage<?>> cache = new ConcurrentHashMap<>();

    @Autowired
    public DefaultOpsDrillService(BlueconeOpsProperties properties,
                                  OutboxMessageRepository outboxMessageRepository,
                                  EventConsumeRecordOpsRepository consumeOpsRepository,
                                  IdempotencyRecordOpsRepository idemOpsRepository) {
        this.properties = properties;
        this.outboxMessageRepository = outboxMessageRepository;
        this.consumeOpsRepository = consumeOpsRepository;
        this.idemOpsRepository = idemOpsRepository;
        this.clock = Clock.systemUTC();
    }

    @Override
    public PageResult<OutboxItem> listOutbox(String status, Long beforeId, int limit) {
        int effectiveLimit = clampLimit(limit);
        String normalizedStatus = normalize(status);
        List<OutboxMessageStatus> statuses = mapOutboxStatuses(normalizedStatus);

        String cacheKey = "outbox|" + normalizedStatus + "|" + (beforeId == null ? "" : beforeId) + "|" + effectiveLimit;
        PageResult<OutboxItem> cached = getCached(cacheKey);
        if (cached != null) {
            return cached;
        }

        boolean includePayload = properties.isExposePayload();
        List<OutboxMessageEntity> entities = outboxMessageRepository.listForOps(
                statuses,
                beforeId,
                effectiveLimit,
                includePayload
        );

        List<OutboxItem> items = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        for (OutboxMessageEntity e : entities) {
            String eventId = truncate(e.getEventKey(), 64);
            String eventType = e.getEventType();
            String aggregateType = deriveAggregateType(eventType);
            String publicAggregateId = null;
            String itemStatus = mapOutboxStatusString(e.getStatus());
            int retryCount = e.getRetryCount() == null ? 0 : e.getRetryCount();
            String nextRetryAt = formatTime(e.getNextRetryAt(), formatter);
            String createdAt = formatTime(e.getCreatedAt(), formatter);

            items.add(new OutboxItem(
                    e.getId() == null ? -1L : e.getId(),
                    eventId,
                    eventType,
                    aggregateType,
                    publicAggregateId,
                    itemStatus,
                    retryCount,
                    nextRetryAt,
                    null,
                    null,
                    null,
                    createdAt
            ));
        }

        String nextCursor = computeNextCursorFromIds(
                items.stream().map(OutboxItem::id).toList());
        PageResult<OutboxItem> page = new PageResult<>(items, nextCursor, effectiveLimit);
        putCached(cacheKey, page);
        return page;
    }

    @Override
    public PageResult<ConsumeItem> listConsume(String consumerGroup, String status, Long beforeId, int limit) {
        int effectiveLimit = clampLimit(limit);
        String normalizedStatus = normalize(status);
        Integer statusCode = mapConsumeStatus(normalizedStatus);
        boolean retryOnly = "RETRY".equals(normalizedStatus);

        String groupKey = consumerGroup == null ? "" : consumerGroup;
        String cacheKey = "consume|" + groupKey + "|" + normalizedStatus + "|" + (beforeId == null ? "" : beforeId) + "|" + effectiveLimit;

        PageResult<ConsumeItem> cached = getCached(cacheKey);
        if (cached != null) {
            return cached;
        }

        Instant nowInstant = clock.instant();
        LocalDateTime nowLocal = LocalDateTime.ofInstant(nowInstant, ZoneId.systemDefault());
        List<EventConsumeRecordDO> records = consumeOpsRepository.listConsume(
                consumerGroup,
                statusCode,
                retryOnly,
                nowLocal,
                beforeId,
                effectiveLimit
        );

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        List<ConsumeItem> items = new ArrayList<>();
        for (EventConsumeRecordDO r : records) {
            String eventId = ulidToString(r.getEventId());
            String statusStr = mapConsumeStatusString(r.getStatus());
            int retryCount = r.getRetryCount() == null ? 0 : r.getRetryCount();
            String nextRetryAt = formatTime(r.getNextRetryAt(), formatter);
            String lockedUntil = formatTime(r.getLockedUntil(), formatter);
            String createdAt = formatTime(r.getCreatedAt(), formatter);
            String updatedAt = formatTime(r.getUpdatedAt(), formatter);
            String errorMsg = truncate(r.getErrorMsg(), properties.getMaxErrorMsgLen());

            items.add(new ConsumeItem(
                    r.getId() == null ? -1L : r.getId(),
                    r.getConsumerGroup(),
                    eventId,
                    r.getEventType(),
                    statusStr,
                    retryCount,
                    nextRetryAt,
                    lockedUntil,
                    r.getLockedBy(),
                    errorMsg,
                    createdAt,
                    updatedAt
            ));
        }

        String nextCursor = computeNextCursorFromIds(items.stream()
                .map(ConsumeItem::id)
                .toList());
        PageResult<ConsumeItem> page = new PageResult<>(items, nextCursor, effectiveLimit);
        putCached(cacheKey, page);
        return page;
    }

    @Override
    public PageResult<IdemConflictItem> listIdemConflicts(Long beforeId, int limit) {
        int effectiveLimit = clampLimit(limit);
        String cacheKey = "idem|" + (beforeId == null ? "" : beforeId) + "|" + effectiveLimit;

        PageResult<IdemConflictItem> cached = getCached(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<IdempotencyRecordDO> records = idemOpsRepository.listConflicts(beforeId, effectiveLimit);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        List<IdemConflictItem> items = new ArrayList<>();
        for (IdempotencyRecordDO r : records) {
            long id = r.getId() == null ? -1L : r.getId();
            long tenantId = r.getTenantId() == null ? 0L : r.getTenantId();
            String statusStr = mapIdemStatusString(r.getStatus());
            String errorMsg = truncate(r.getErrorMsg(), properties.getMaxErrorMsgLen());
            String createdAt = formatTime(r.getCreatedAt(), formatter);

            items.add(new IdemConflictItem(
                    id,
                    tenantId,
                    r.getBizType(),
                    prefix(r.getIdemKey(), 8),
                    prefix(r.getRequestHash(), 8),
                    statusStr,
                    r.getErrorCode(),
                    errorMsg,
                    createdAt
            ));
        }

        String nextCursor = computeNextCursorFromIds(items.stream()
                .map(IdemConflictItem::id)
                .toList());
        PageResult<IdemConflictItem> page = new PageResult<>(items, nextCursor, effectiveLimit);
        putCached(cacheKey, page);
        return page;
    }

    private int clampLimit(int limit) {
        int max = properties.getMaxPageSize() > 0 ? properties.getMaxPageSize() : 100;
        int effective = limit <= 0 ? 50 : limit;
        if (effective > max) {
            effective = max;
        }
        return effective;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private List<OutboxMessageStatus> mapOutboxStatuses(String status) {
        if (!StringUtils.hasText(status)) {
            return Collections.emptyList();
        }
        return switch (status) {
            case "READY" -> List.of(OutboxMessageStatus.NEW);
            case "FAILED" -> List.of(OutboxMessageStatus.FAILED, OutboxMessageStatus.DEAD);
            case "SENT" -> List.of(OutboxMessageStatus.DONE);
            case "PROCESSING" -> Collections.emptyList(); // No persisted processing state.
            default -> Collections.emptyList();
        };
    }

    private String mapOutboxStatusString(OutboxMessageStatus status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status) {
            case NEW -> "READY";
            case FAILED, DEAD -> "FAILED";
            case DONE -> "SENT";
        };
    }

    private Integer mapConsumeStatus(String status) {
        return switch (status) {
            case "PROCESSING" -> CONSUME_STATUS_PROCESSING;
            case "SUCCEEDED" -> CONSUME_STATUS_SUCCEEDED;
            case "FAILED", "RETRY" -> CONSUME_STATUS_FAILED;
            default -> null;
        };
    }

    private String mapConsumeStatusString(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status) {
            case CONSUME_STATUS_PROCESSING -> "PROCESSING";
            case CONSUME_STATUS_SUCCEEDED -> "SUCCEEDED";
            case CONSUME_STATUS_FAILED -> "FAILED";
            default -> "UNKNOWN";
        };
    }

    private String mapIdemStatusString(Integer status) {
        if (status == null) {
            return "PROCESSING";
        }
        return switch (status) {
            case 0 -> "PROCESSING";
            case 1 -> "SUCCEEDED";
            case 2 -> "FAILED";
            default -> "PROCESSING";
        };
    }

    private String deriveAggregateType(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return null;
        }
        int dot = eventType.indexOf('.');
        if (dot <= 0) {
            return null;
        }
        return eventType.substring(0, dot);
    }

    private String formatTime(LocalDateTime time, DateTimeFormatter formatter) {
        if (time == null) {
            return null;
        }
        return time.format(formatter);
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        int effectiveMax = maxLen > 0 ? maxLen : value.length();
        if (value.length() <= effectiveMax) {
            return value;
        }
        return value.substring(0, effectiveMax);
    }

    private String prefix(String value, int len) {
        if (value == null) {
            return null;
        }
        if (value.length() <= len) {
            return value;
        }
        return value.substring(0, len);
    }

    private String ulidToString(Ulid128 ulid) {
        return ulid == null ? null : ulid.toString();
    }

    private String computeNextCursorFromIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        long minId = ids.stream()
                .min(Comparator.naturalOrder())
                .orElse(-1L);
        return minId > 0 ? Long.toString(minId) : null;
    }

    @SuppressWarnings("unchecked")
    private <T> PageResult<T> getCached(String key) {
        CachedPage<?> cached = cache.get(key);
        if (cached == null) {
            return null;
        }
        Instant now = clock.instant();
        if (cached.expiresAt().isBefore(now)) {
            cache.remove(key);
            return null;
        }
        return (PageResult<T>) cached.page();
    }

    private void putCached(String key, PageResult<?> page) {
        Duration ttl = properties.getDrillCacheTtl() != null
                ? properties.getDrillCacheTtl()
                : Duration.ofSeconds(1);
        Instant expiresAt = clock.instant().plus(ttl);
        cache.put(key, new CachedPage<>(page, expiresAt));
    }

    private record CachedPage<T>(PageResult<T> page, Instant expiresAt) {
    }
}
