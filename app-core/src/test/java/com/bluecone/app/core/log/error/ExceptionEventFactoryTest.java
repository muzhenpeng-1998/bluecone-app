package com.bluecone.app.core.log.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

class ExceptionEventFactoryTest {

    private final ExceptionEventFactory factory = new ExceptionEventFactory();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void buildsEventFromBusinessExceptionAndHttpContext() throws NoSuchMethodException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/42");
        request.setQueryString("expand=items");
        request.addParameter("expand", "items");
        request.addHeader("User-Agent", "JUnit");
        request.addHeader("X-Forwarded-For", "203.0.113.10");
        request.addHeader("X-Tenant-Id", "321");
        HandlerMethod handlerMethod = new HandlerMethod(new DummyController(), DummyController.class.getMethod("handle"));
        request.setAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE, handlerMethod);

        MDC.put("traceId", "trace-abc");
        MDC.put("requestId", "req-123");
        MDC.put("tenantId", "tenant-test");
        MDC.put("userId", "user-9");

        BusinessException exception = BusinessException.of(ErrorCode.PARAM_INVALID.getCode(), "orderId missing");

        ExceptionEvent event = factory.fromException(exception, request, HttpStatus.BAD_REQUEST.value());

        assertThat(event.getEventType()).isEqualTo("API_ERROR");
        assertThat(event.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(event.getErrorCode()).isEqualTo(ErrorCode.PARAM_INVALID.getCode());
        assertThat(event.getPath()).isEqualTo("/api/orders/42");
        assertThat(event.getController()).isEqualTo("DummyController");
        assertThat(event.getHandler()).isEqualTo("handle");
        assertThat(event.getTraceId()).isEqualTo("trace-abc");
        assertThat(event.getClientIp()).isEqualTo("203.0.113.10");
        assertThat(event.getStackTop()).isNotNull();
        assertThat(event.getSeverity()).isEqualTo(ExceptionSeverity.WARN);
    }

    @Test
    void resolvesCriticalSeverityForServerErrors() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        RuntimeException exception = new RuntimeException("boom");

        ExceptionEvent event = factory.fromException(exception, request, HttpStatus.INTERNAL_SERVER_ERROR.value());

        assertThat(event.getSeverity()).isEqualTo(ExceptionSeverity.CRITICAL);
        assertThat(event.getEventType()).isEqualTo("API_ERROR");
        assertThat(event.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode());
        assertThat(event.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    static class DummyController {
        public void handle() {
            // no-op
        }
    }
}
