package com.bluecone.app.cacheinval;

import com.bluecone.app.config.CacheInvalidationProperties;
import com.bluecone.app.core.cacheepoch.api.CacheEpochProvider;
import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;
import com.bluecone.app.core.cacheinval.application.CacheInvalidationExecutor;
import com.bluecone.app.core.cacheinval.observability.api.CacheInvalidationLogEntry;
import com.bluecone.app.core.cacheinval.observability.api.CacheInvalidationLogWriter;
import com.bluecone.app.core.cacheinval.transport.CacheInvalidationBus;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Subscribes to {@link CacheInvalidationBus} and executes invalidations locally.
 *
 * <p>Includes a lightweight best-effort de-duplication based on eventId to avoid
 * processing the same event too frequently.</p>
 */
@Slf4j
public class CacheInvalidationListener {

    private final CacheInvalidationExecutor executor;
    private final CacheInvalidationBus bus;
    private final CacheInvalidationProperties properties;
    private final CacheInvalidationLogWriter logWriter;
    private final String transport;
    private final String instanceId;
    private final CacheEpochProvider epochProvider;

    private final Map<String, Instant> recentEvents = new ConcurrentHashMap<>();

    public CacheInvalidationListener(CacheInvalidationExecutor executor,
                                     CacheInvalidationBus bus,
                                     CacheInvalidationProperties properties,
                                     CacheInvalidationLogWriter logWriter,
                                     String instanceId,
                                     String transport,
                                     CacheEpochProvider epochProvider) {
        this.executor = executor;
        this.bus = bus;
        this.properties = properties;
        this.logWriter = logWriter != null ? logWriter : CacheInvalidationLogWriter.NOOP;
        this.instanceId = instanceId;
        this.transport = transport;
        this.epochProvider = epochProvider;
        this.bus.subscribe(this::onEvent);
    }

    private void onEvent(CacheInvalidationEvent event) {
        if (event == null) {
            return;
        }
        boolean duplicate = isDuplicate(event);
        try {
            if (event.epochBump()) {
                if (epochProvider != null) {
                    long tenantId = event.tenantId();
                    String namespace = event.namespace();
                    Long newEpoch = event.newEpoch();
                    if (newEpoch != null && newEpoch > 0L) {
                        epochProvider.updateLocalEpoch(tenantId, namespace, newEpoch);
                    } else {
                        epochProvider.bumpEpoch(tenantId, namespace);
                    }
                }
            } else {
                executor.execute(event);
            }
            if (!duplicate) {
                writeLog(event, "OK", null);
            }
        } catch (Exception ex) {
            log.warn("[CacheInvalidation] execution failed for eventId={}", event.eventId(), ex);
            if (!duplicate) {
                String note = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                writeLog(event, "ERROR", note);
            }
        }
    }

    private boolean isDuplicate(CacheInvalidationEvent event) {
        String id = event.eventId();
        if (id == null || id.isBlank()) {
            return false;
        }
        Instant now = Instant.now();
        Instant prev = recentEvents.putIfAbsent(id, now);
        Duration ttl = properties.getRecentEventTtl();
        if (prev != null && Duration.between(prev, now).compareTo(ttl) < 0) {
            return true;
        }
        // best-effort cleanup when map grows
        if (recentEvents.size() > 1000) {
            Instant threshold = now.minus(ttl);
            recentEvents.entrySet().removeIf(e -> e.getValue().isBefore(threshold));
        }
        return false;
    }

    private void writeLog(CacheInvalidationEvent event, String result, String note) {
        Instant now = Instant.now();
        Instant occurredAt = event.occurredAt() != null ? event.occurredAt() : now;
        List<String> samples = buildKeySampleHashes(event.keys());
        CacheInvalidationLogEntry entry = new CacheInvalidationLogEntry(
                occurredAt,
                now,
                event.tenantId(),
                event.scope().name(),
                event.namespace(),
                event.eventId(),
                event.keys() != null ? event.keys().size() : 0,
                samples,
                event.configVersion() == 0L ? null : event.configVersion(),
                transport,
                instanceId,
                result,
                note,
                event.protectionHint(),
                event.epochBump(),
                event.newEpoch()
        );
        logWriter.write(entry);
    }

    private List<String> buildKeySampleHashes(List<String> keys) {
        List<String> result = new ArrayList<>();
        if (keys == null || keys.isEmpty()) {
            return result;
        }
        int limit = Math.min(5, keys.size());
        for (int i = 0; i < limit; i++) {
            String key = keys.get(i);
            if (key == null || key.isBlank()) {
                continue;
            }
            result.add(hashPrefix(key));
        }
        return result;
    }

    private String hashPrefix(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(4, bytes.length); i++) {
                String hex = Integer.toHexString(bytes[i] & 0xff);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            return "na";
        }
    }
}
