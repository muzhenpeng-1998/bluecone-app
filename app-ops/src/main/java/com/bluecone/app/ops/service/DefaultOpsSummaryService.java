package com.bluecone.app.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.infra.event.consume.EventConsumeRecordDO;
import com.bluecone.app.infra.event.consume.EventConsumeRecordMapper;
import com.bluecone.app.infra.idempotency.IdempotencyRecordDO;
import com.bluecone.app.infra.idempotency.IdempotencyRecordMapper;
import com.bluecone.app.infra.outbox.entity.OutboxMessageStatus;
import com.bluecone.app.infra.outbox.repository.OutboxMessageRepository;
import com.bluecone.app.ops.api.dto.ConsumeGroupSummary;
import com.bluecone.app.ops.api.dto.ConsumeSummary;
import com.bluecone.app.ops.api.dto.CreateSummary;
import com.bluecone.app.ops.api.dto.IdempotencySummary;
import com.bluecone.app.ops.api.dto.OpsSummary;
import com.bluecone.app.ops.api.dto.OutboxSummary;
import com.bluecone.app.ops.api.dto.SystemSummary;
import com.bluecone.app.ops.config.BlueconeOpsProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class DefaultOpsSummaryService implements OpsSummaryService {

    private static final int STATUS_PROCESSING = 0;
    private static final int STATUS_SUCCEEDED = 1;
    private static final int STATUS_FAILED = 2;

    private final BlueconeOpsProperties properties;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;
    private final OutboxMessageRepository outboxMessageRepository;
    private final EventConsumeRecordMapper eventConsumeRecordMapper;
    private final IdempotencyRecordMapper idempotencyRecordMapper;
    private final Environment environment;
    private final Clock clock;

    private final AtomicReference<CachedSummary> cache = new AtomicReference<>();

    @Autowired
    public DefaultOpsSummaryService(BlueconeOpsProperties properties,
                                    ObjectProvider<MeterRegistry> meterRegistryProvider,
                                    OutboxMessageRepository outboxMessageRepository,
                                    EventConsumeRecordMapper eventConsumeRecordMapper,
                                    IdempotencyRecordMapper idempotencyRecordMapper,
                                    Environment environment) {
        this.properties = properties;
        this.meterRegistryProvider = meterRegistryProvider;
        this.outboxMessageRepository = outboxMessageRepository;
        this.eventConsumeRecordMapper = eventConsumeRecordMapper;
        this.idempotencyRecordMapper = idempotencyRecordMapper;
        this.environment = environment;
        this.clock = Clock.systemUTC();
    }

    @Override
    public OpsSummary getSummary() {
        Instant now = clock.instant();
        Duration ttl = properties.getCacheTtl() != null ? properties.getCacheTtl() : Duration.ofSeconds(2);

        CachedSummary cached = cache.get();
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.summary();
        }

        OpsSummary summary = buildSummary(now);
        cache.set(new CachedSummary(summary, now.plus(ttl)));
        return summary;
    }

    private OpsSummary buildSummary(Instant now) {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMxBean.getHeapMemoryUsage();

        long uptimeSeconds = runtimeMxBean.getUptime() / 1000;
        int threads = threadMxBean.getThreadCount();
        long heapUsedBytes = heapUsage.getUsed();
        long heapMaxBytes = heapUsage.getMax();

        SystemSummary systemSummary = new SystemSummary(
                uptimeSeconds,
                threads,
                heapUsedBytes,
                heapMaxBytes
        );

        OutboxSummary outboxSummary = buildOutboxSummary(now);
        ConsumeSummary consumeSummary = buildConsumeSummary(now);
        IdempotencySummary idempotencySummary = buildIdempotencySummary(now);
        CreateSummary createSummary = buildCreateSummary();

        String instanceId = runtimeMxBean.getName(); // pid@hostname
        String appName = environment.getProperty("spring.application.name", "bluecone-app");
        String version = environment.getProperty("bluecone.app.version", "unknown");
        String startedAt = Instant.ofEpochMilli(runtimeMxBean.getStartTime())
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        return new OpsSummary(
                instanceId,
                appName,
                version,
                startedAt,
                systemSummary,
                outboxSummary,
                consumeSummary,
                idempotencySummary,
                createSummary
        );
    }

    private OutboxSummary buildOutboxSummary(Instant now) {
        long ready = outboxMessageRepository.countByStatusAndPrefix(OutboxMessageStatus.NEW, null, null);
        long failed = outboxMessageRepository.countByStatusAndPrefix(OutboxMessageStatus.FAILED, null, null)
                + outboxMessageRepository.countByStatusAndPrefix(OutboxMessageStatus.DEAD, null, null);

        // Processing state is not persisted in this Outbox implementation.
        long processing = -1;

        LocalDateTime oldestCreatedAt = outboxMessageRepository.findOldestCreatedAtByStatus(OutboxMessageStatus.NEW);
        double oldestAgeSeconds;
        if (oldestCreatedAt == null) {
            oldestAgeSeconds = -1;
        } else {
            LocalDateTime nowLocal = LocalDateTime.ofInstant(now, ZoneId.systemDefault());
            oldestAgeSeconds = Duration.between(oldestCreatedAt, nowLocal).toSeconds();
        }

        // In-process mode: we do not compute short-window rates from counters.
        double sendSuccessRate5m = -1;
        double sendFailureRate5m = -1;

        List<String> recentErrors = fetchRecentConsumeErrors();

        return new OutboxSummary(
                ready,
                processing,
                failed,
                oldestAgeSeconds,
                sendSuccessRate5m,
                sendFailureRate5m,
                recentErrors
        );
    }

    private ConsumeSummary buildConsumeSummary(Instant now) {
        LocalDateTime nowLocal = LocalDateTime.ofInstant(now, ZoneId.systemDefault());
        LocalDateTime fiveMinutesAgo = nowLocal.minusMinutes(5);

        LambdaQueryWrapper<EventConsumeRecordDO> recentWrapper = new LambdaQueryWrapper<>();
        recentWrapper.select(
                        EventConsumeRecordDO::getConsumerGroup,
                        EventConsumeRecordDO::getStatus,
                        EventConsumeRecordDO::getCreatedAt,
                        EventConsumeRecordDO::getProcessedAt,
                        EventConsumeRecordDO::getErrorMsg
                )
                .ge(EventConsumeRecordDO::getProcessedAt, fiveMinutesAgo);

        List<EventConsumeRecordDO> recentRecords = eventConsumeRecordMapper.selectList(recentWrapper);

        Map<String, List<EventConsumeRecordDO>> byGroup = recentRecords.stream()
                .filter(r -> r.getConsumerGroup() != null)
                .collect(Collectors.groupingBy(EventConsumeRecordDO::getConsumerGroup));

        List<ConsumeGroupSummary> groups = new ArrayList<>();
        for (Map.Entry<String, List<EventConsumeRecordDO>> entry : byGroup.entrySet()) {
            String group = entry.getKey();
            List<EventConsumeRecordDO> records = entry.getValue();

            long successCount = records.stream()
                    .filter(r -> Objects.equals(r.getStatus(), STATUS_SUCCEEDED))
                    .count();
            long failureCount = records.stream()
                    .filter(r -> Objects.equals(r.getStatus(), STATUS_FAILED))
                    .count();
            long total = successCount + failureCount;

            double successRate = total == 0 ? -1 : (double) successCount / total;
            double failureRate = total == 0 ? -1 : (double) failureCount / total;

            List<Long> latencies = records.stream()
                    .filter(r -> r.getCreatedAt() != null && r.getProcessedAt() != null)
                    .map(r -> Duration.between(r.getCreatedAt(), r.getProcessedAt()).toMillis())
                    .filter(l -> l >= 0)
                    .toList();

            double avgLatency = -1;
            double p95Latency = -1;
            if (!latencies.isEmpty()) {
                long sum = 0;
                for (Long v : latencies) {
                    sum += v;
                }
                avgLatency = (double) sum / latencies.size();

                List<Long> sorted = new ArrayList<>(latencies);
                sorted.sort(Comparator.naturalOrder());
                int index = (int) Math.ceil(sorted.size() * 0.95) - 1;
                if (index < 0) {
                    index = 0;
                }
                p95Latency = sorted.get(index);
            }

            groups.add(new ConsumeGroupSummary(group, successRate, failureRate, avgLatency, p95Latency));
        }

        // retryReady: FAILED and nextRetryAt <= now
        LambdaQueryWrapper<EventConsumeRecordDO> retryWrapper = new LambdaQueryWrapper<>();
        retryWrapper.eq(EventConsumeRecordDO::getStatus, STATUS_FAILED)
                .le(EventConsumeRecordDO::getNextRetryAt, nowLocal);
        long retryReady = eventConsumeRecordMapper.selectCount(retryWrapper);

        // inProgressLocks: PROCESSING and lockedUntil > now
        LambdaQueryWrapper<EventConsumeRecordDO> inProgressWrapper = new LambdaQueryWrapper<>();
        inProgressWrapper.eq(EventConsumeRecordDO::getStatus, STATUS_PROCESSING)
                .gt(EventConsumeRecordDO::getLockedUntil, nowLocal);
        long inProgressLocks = eventConsumeRecordMapper.selectCount(inProgressWrapper);

        return new ConsumeSummary(groups, retryReady, inProgressLocks);
    }

    private List<String> fetchRecentConsumeErrors() {
        LambdaQueryWrapper<EventConsumeRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(EventConsumeRecordDO::getErrorMsg)
                .eq(EventConsumeRecordDO::getStatus, STATUS_FAILED)
                .isNotNull(EventConsumeRecordDO::getErrorMsg)
                .orderByDesc(EventConsumeRecordDO::getUpdatedAt)
                .last("limit 5");
        List<EventConsumeRecordDO> records = eventConsumeRecordMapper.selectList(wrapper);
        List<String> errors = new ArrayList<>();
        for (EventConsumeRecordDO record : records) {
            if (record.getErrorMsg() != null) {
                errors.add(record.getErrorMsg());
            }
        }
        return errors;
    }

    private IdempotencySummary buildIdempotencySummary(Instant now) {
        LocalDateTime nowLocal = LocalDateTime.ofInstant(now, ZoneId.systemDefault());
        LocalDateTime fiveMinutesAgo = nowLocal.minusMinutes(5);

        // conflictRate10m: conflicts are not stored as separate rows in bc_idempotency, so mark as N/A.
        double conflictRate10m = -1;

        // inProgressRate5m: compute ratio of PROCESSING records among all records updated in last 5 minutes.
        LambdaQueryWrapper<IdempotencyRecordDO> recentWrapper = new LambdaQueryWrapper<>();
        recentWrapper.select(IdempotencyRecordDO::getStatus)
                .ge(IdempotencyRecordDO::getUpdatedAt, fiveMinutesAgo);
        List<IdempotencyRecordDO> recentRecords = idempotencyRecordMapper.selectList(recentWrapper);

        long total = recentRecords.size();
        long processingCount = recentRecords.stream()
                .filter(r -> r.getStatus() != null && r.getStatus() == 0)
                .count();
        double inProgressRate5m = total == 0 ? -1 : (double) processingCount / total;

        // avgLatencyMs5m: average between createdAt and updatedAt for SUCCEEDED records in last 5 minutes.
        LambdaQueryWrapper<IdempotencyRecordDO> successWrapper = new LambdaQueryWrapper<>();
        successWrapper.select(IdempotencyRecordDO::getCreatedAt, IdempotencyRecordDO::getUpdatedAt)
                .eq(IdempotencyRecordDO::getStatus, 1)
                .ge(IdempotencyRecordDO::getUpdatedAt, fiveMinutesAgo);
        List<IdempotencyRecordDO> successRecords = idempotencyRecordMapper.selectList(successWrapper);

        double avgLatencyMs5m = -1;
        if (!successRecords.isEmpty()) {
            long sum = 0;
            long count = 0;
            for (IdempotencyRecordDO record : successRecords) {
                if (record.getCreatedAt() != null && record.getUpdatedAt() != null) {
                    long millis = Duration.between(record.getCreatedAt(), record.getUpdatedAt()).toMillis();
                    if (millis >= 0) {
                        sum += millis;
                        count++;
                    }
                }
            }
            if (count > 0) {
                avgLatencyMs5m = (double) sum / count;
            }
        }

        return new IdempotencySummary(conflictRate10m, inProgressRate5m, avgLatencyMs5m);
    }

    private CreateSummary buildCreateSummary() {
        // There is currently no dedicated metrics source for create operations.
        // Mark all fields as N/A using -1.
        return new CreateSummary(-1, -1, -1);
    }

    private record CachedSummary(OpsSummary summary, Instant expiresAt) {
    }
}
