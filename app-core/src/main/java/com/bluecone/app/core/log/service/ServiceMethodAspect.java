package com.bluecone.app.core.log.service;

import com.bluecone.app.core.log.ApiEvent;
import com.bluecone.app.core.log.LogProperties;
import com.bluecone.app.core.log.pipeline.EventPipeline;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Service 层自动追踪切面：零侵入采集成功/异常事件并走统一事件管道。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class ServiceMethodAspect {

    private final ServiceEventFactory factory;
    private final ServiceEventEnricher enricher;
    private final EventPipeline eventPipeline;
    private final LogProperties logProperties;

    @Around("@within(org.springframework.stereotype.Service)")
    public Object traceService(ProceedingJoinPoint pjp) throws Throwable {
        if (!logProperties.isEnabled()) {
            return pjp.proceed();
        }
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - start;
            ApiEvent event = enricher.enrich(factory.successEvent(pjp, result, elapsed));
            eventPipeline.process(event);
            return result;
        } catch (Throwable ex) {
            long elapsed = System.currentTimeMillis() - start;
            ApiEvent event = enricher.enrich(factory.exceptionEvent(pjp, ex, elapsed));
            eventPipeline.process(event);
            throw ex;
        }
    }
}
