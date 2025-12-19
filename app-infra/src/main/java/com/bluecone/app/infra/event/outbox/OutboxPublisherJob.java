package com.bluecone.app.infra.event.outbox;

import com.bluecone.app.infra.scheduler.annotation.BlueconeJob;
import com.bluecone.app.infra.scheduler.core.JobContext;
import com.bluecone.app.infra.scheduler.core.JobHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Outbox 事件发布定时任务
 * 扫描待投递的事件并投递到 InProcessEventDispatcher
 */
@Slf4j
@Component
@BlueconeJob(
        code = "outbox_publisher", 
        name = "Outbox Event Publisher", 
        cron = "0/10 * * * * ?",  // 每 10 秒执行一次
        timeoutSeconds = 30
)
@RequiredArgsConstructor
public class OutboxPublisherJob implements JobHandler {
    
    private final OutboxEventService outboxEventService;
    private final InProcessEventDispatcher eventDispatcher;
    
    /**
     * 每批次处理的事件数量
     */
    private static final int BATCH_SIZE = 100;
    
    @Override
    public void handle(JobContext context) {
        String traceId = context.getTraceId();
        log.debug("[OutboxPublisher] Starting event publishing, traceId={}", traceId);
        
        try {
            // 查询待投递的事件
            List<OutboxEventPO> pendingEvents = outboxEventService.findPendingEvents(BATCH_SIZE);
            
            if (pendingEvents.isEmpty()) {
                log.debug("[OutboxPublisher] No pending events found, traceId={}", traceId);
                return;
            }
            
            log.info("[OutboxPublisher] Found {} pending events, traceId={}", pendingEvents.size(), traceId);
            
            // 逐个投递事件
            int successCount = 0;
            int failureCount = 0;
            
            for (OutboxEventPO event : pendingEvents) {
                try {
                    // 投递事件到 InProcessEventDispatcher
                    eventDispatcher.dispatch(event);
                    
                    // 标记为已投递
                    outboxEventService.markAsSent(event.getEventId());
                    
                    successCount++;
                    
                } catch (Exception e) {
                    // 标记为失败（用于重试）
                    String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    outboxEventService.markAsFailed(event.getEventId(), errorMsg);
                    
                    failureCount++;
                    
                    log.error("[OutboxPublisher] Failed to dispatch event: eventId={}, eventType={}, error={}", 
                            event.getEventId(), event.getEventType(), errorMsg, e);
                }
            }
            
            log.info("[OutboxPublisher] Event publishing completed: success={}, failure={}, traceId={}", 
                    successCount, failureCount, traceId);
            
        } catch (Exception e) {
            log.error("[OutboxPublisher] Event publishing failed: traceId={}", traceId, e);
        }
    }
}
