package com.bluecone.app.infra.cacheinval;

import com.bluecone.app.core.cacheinval.observability.api.CacheInvalidationLogEntry;
import com.bluecone.app.core.cacheinval.observability.api.CacheInvalidationLogWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationLogWriterImpl implements CacheInvalidationLogWriter {

    private final CacheInvalidationLogMapper mapper;

    @Override
    public void write(CacheInvalidationLogEntry entry) {
        if (entry == null) {
            return;
        }
        try {
            CacheInvalidationLogDO row = new CacheInvalidationLogDO();
            row.setOccurredAt(toLocal(entry.occurredAt() != null ? entry.occurredAt() : entry.receivedAt()));
            row.setReceivedAt(toLocal(entry.receivedAt()));
            row.setTenantId(entry.tenantId());
            row.setScope(truncate(entry.scope(), 32));
            row.setNamespace(truncate(entry.namespace(), 64));
            row.setEventId(truncate(entry.eventId(), 32));
            row.setKeysCount(entry.keysCount());
            row.setKeySamples(buildKeySamples(entry.keySampleHashes()));
            row.setConfigVersion(entry.configVersion());
            row.setTransport(truncate(entry.transport(), 32));
            row.setInstanceId(truncate(entry.instanceId(), 64));
            row.setResult(truncate(entry.result(), 16));
            row.setNote(truncate(entry.note(), 256));
            row.setDecision(truncate(entry.decision(), 16));
            row.setStormMode(entry.stormMode() ? 1 : 0);
            row.setEpoch(entry.epoch());
            mapper.insert(row);
        } catch (Exception ex) {
            log.warn("[CacheInvalidationLog] failed to persist log entry eventId={} tenantId={}",
                    entry.eventId(), entry.tenantId(), ex);
        }
    }

    private LocalDateTime toLocal(Instant instant) {
        if (instant == null) {
            return LocalDateTime.now(ZoneOffset.UTC);
        }
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private String buildKeySamples(List<String> hashes) {
        if (hashes == null || hashes.isEmpty()) {
            return null;
        }
        String joined = hashes.stream()
                .filter(s -> s != null && !s.isBlank())
                .limit(5)
                .map(s -> s.length() > 16 ? s.substring(0, 16) : s)
                .collect(Collectors.joining(","));
        return truncate(joined, 512);
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }
}
