package com.bluecone.app.resource.domain.service;

import com.bluecone.app.infra.redis.core.RedisOps;
import com.bluecone.app.resource.api.enums.ResourceProfileCode;
import com.bluecone.app.resource.api.exception.ResourceUploadException;
import com.bluecone.app.resource.config.TenantResourceQuotaProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 租户级资源配额服务，负责记录 & 限制每日上传次数和总字节数，并产生日志/指标。
 */
@Component
public class TenantResourceQuotaService {

    private static final Logger log = LoggerFactory.getLogger(TenantResourceQuotaService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final TenantResourceQuotaProperties properties;
    private final RedisOps redisOps;
    private final MeterRegistry meterRegistry;

    public TenantResourceQuotaService(TenantResourceQuotaProperties properties,
                                      RedisOps redisOps,
                                      MeterRegistry meterRegistry) {
        this.properties = properties;
        this.redisOps = redisOps;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 请求上传策略前预检查当前租户剩余配额。
     */
    public void assertCanUpload(long tenantId, long expectedSizeBytes, ResourceProfileCode profileCode) {
        String daySuffix = currentDaySuffix();
        long countLimit = properties.getDailyUploadCountLimit(tenantId);
        long bytesLimit = properties.getDailyUploadBytesLimit(tenantId);

        String countKey = buildCountKey(tenantId, daySuffix);
        long currentCount = parseLong(redisOps.getString(countKey));
        if (countLimit > 0 && currentCount + 1 > countLimit) {
            meterRegistry.counter("resource.quota.exceeded",
                    Tags.of("tenant", String.valueOf(tenantId), "profile", profileCode.name(), "type", "count"))
                    .increment();
            log.warn("[ResourceQuotaExceeded] tenant={} profile={} type=count count={} limit={}",
                    tenantId, profileCode, currentCount, countLimit);
            throw new ResourceUploadException(ResourceUploadException.RES_QUOTA_DAILY_COUNT_EXCEEDED,
                    "当前租户今日上传次数已达上限");
        }

        long expected = Math.max(0, expectedSizeBytes);
        if (bytesLimit > 0 && expected > 0) {
            String bytesKey = buildBytesKey(tenantId, daySuffix);
            long currentBytes = parseLong(redisOps.getString(bytesKey));
            if (currentBytes + expected > bytesLimit) {
                meterRegistry.counter("resource.quota.exceeded",
                        Tags.of("tenant", String.valueOf(tenantId), "profile", profileCode.name(), "type", "bytes"))
                        .increment();
                log.warn("[ResourceQuotaExceeded] tenant={} profile={} type=bytes bytes={} limit={}",
                        tenantId, profileCode, currentBytes, bytesLimit);
                throw new ResourceUploadException(ResourceUploadException.RES_QUOTA_DAILY_BYTES_EXCEEDED,
                        "当前租户今日上传存储空间已达上限");
            }
        }
    }

    /**
     * 上传完成后累计当天计数/字节数。
     */
    public void consumeQuota(long tenantId, long actualSizeBytes, ResourceProfileCode profileCode) {
        String daySuffix = currentDaySuffix();
        Duration ttl = quotaKeyTtl();

        String countKey = buildCountKey(tenantId, daySuffix);
        long countValue = redisOps.incr(countKey, 1);
        if (countValue == 1) {
            redisOps.expire(countKey, ttl);
        }
        log.debug("租户上传次数累加 tenant={} profile={} countKey={} value={}",
                tenantId, profileCode, countKey, countValue);

        if (actualSizeBytes > 0) {
            String bytesKey = buildBytesKey(tenantId, daySuffix);
            long bytesValue = redisOps.incr(bytesKey, actualSizeBytes);
            if (bytesValue == actualSizeBytes) {
                redisOps.expire(bytesKey, ttl);
            }
            log.debug("租户上传流量累加 tenant={} profile={} bytesKey={} value={} bytes",
                    tenantId, profileCode, bytesKey, bytesValue);
        }
    }

    private String currentDaySuffix() {
        return LocalDate.now(ZONE).format(DATE_FORMATTER);
    }

    private static Duration quotaKeyTtl() {
        LocalDateTime now = LocalDateTime.now(ZONE);
        LocalDateTime tomorrow = now.toLocalDate().plusDays(1).atStartOfDay();
        Duration duration = Duration.between(now, tomorrow.plusHours(2));
        return duration.isNegative() ? Duration.ofHours(2) : duration;
    }

    private static String buildCountKey(long tenantId, String suffix) {
        return String.format("res:quota:%d:cnt:%s", tenantId, suffix);
    }

    private static String buildBytesKey(long tenantId, String suffix) {
        return String.format("res:quota:%d:bytes:%s", tenantId, suffix);
    }

    private static long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
