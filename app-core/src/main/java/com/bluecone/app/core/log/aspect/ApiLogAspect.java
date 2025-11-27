package com.bluecone.app.core.log.aspect;

import com.bluecone.app.core.log.ApiEvent;
import com.bluecone.app.core.log.ApiEventFactory;
import com.bluecone.app.core.log.annotation.ApiLog;
import com.bluecone.app.core.log.pipeline.EventPipeline;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 事件驱动版 API 日志切面：进入/退出/异常均输出事件。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class ApiLogAspect {

    private final ApiEventFactory eventFactory;
    private final EventPipeline eventPipeline;
    private final ThreadLocal<ApiEvent> eventHolder = new ThreadLocal<>();

    @Before(value = "@annotation(apiLog)", argNames = "joinPoint,apiLog")
    public void onEnter(JoinPoint joinPoint, ApiLog apiLog) {
        HttpServletRequest request = currentRequest();
        ApiEvent event = eventFactory.createStartEvent(request)
                .setEventName(apiLog.value());
        if (apiLog.includePayload()) {
            event.setPayload(joinPoint.getArgs());
        }
        eventHolder.set(event);
        eventPipeline.process(event);
    }

    @AfterReturning(value = "@annotation(apiLog)", returning = "result", argNames = "apiLog,result")
    public void onReturn(ApiLog apiLog, Object result) {
        ApiEvent event = takeEvent();
        if (event == null) {
            return;
        }
        if (apiLog.includePayload()) {
            event.setPayload(result);
        }
        eventFactory.finalizeEvent(event, result, null);
        eventPipeline.process(event);
    }

    @AfterThrowing(value = "@annotation(apiLog)", throwing = "throwable", argNames = "apiLog,throwable")
    public void onThrow(ApiLog apiLog, Throwable throwable) {
        ApiEvent event = takeEvent();
        if (event == null) {
            event = eventFactory.createStartEvent(currentRequest())
                    .setEventName(apiLog.value());
        }
        eventFactory.finalizeEvent(event, null, asException(throwable));
        eventPipeline.process(event);
    }

    private ApiEvent takeEvent() {
        ApiEvent event = eventHolder.get();
        eventHolder.remove();
        return event;
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private Exception asException(Throwable throwable) {
        if (throwable instanceof Exception ex) {
            return ex;
        }
        return new RuntimeException(throwable);
    }
}
