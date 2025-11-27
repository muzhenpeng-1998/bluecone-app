package com.bluecone.app.core.log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Instant;

/**
 * 统一 API 事件对象，面向事件驱动日志的最小结构化单元。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiEvent {

    public enum EventType {
        API_START, API_END, API_ERROR
    }

    public enum Status {
        SUCCESS, FAILED
    }

    private EventType eventType;
    private String eventName;
    private String path;
    private String method;
    private String version;
    private String node;
    private Status status;
    private Long latencyMs;
    private String traceId;
    private String requestId;
    private String tenantId;
    private String userId;
    private String ip;
    private String userAgent;
    private String deviceId;
    private String requestBodyDigest;
    private Long responseSize;
    private String exceptionDigest;
    private Instant timestamp;
    private Object payload;

    @JsonIgnore
    private Long startTimeMs;
}
