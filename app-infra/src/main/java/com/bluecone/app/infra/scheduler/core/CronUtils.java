package com.bluecone.app.infra.scheduler.core;

import java.time.LocalDateTime;

import org.springframework.scheduling.support.CronExpression;

/**
 * Cron 工具，基于 Spring Quartz 风格解析。
 */
public final class CronUtils {

    private CronUtils() {
    }

    public static LocalDateTime nextTime(String cron, LocalDateTime from) {
        CronExpression expression = CronExpression.parse(cron);
        return expression.next(from);
    }

    public static boolean isDue(LocalDateTime nextRunAt, LocalDateTime now) {
        return nextRunAt != null && !nextRunAt.isAfter(now);
    }
}
