package com.bluecone.app.core.log.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.List;

/**
 * 异常事件模型：承载一次异常的完整上下文，便于结构化存储与查询。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExceptionEvent {

    private String eventType;
    private Instant timestamp;
    private ExceptionSeverity severity;

    // 上下文
    private String traceId;
    private String requestId;
    private String tenantId;
    private String userId;

    // HTTP
    private String path;
    private String method;
    private String queryString;
    private Integer httpStatus;

    // 代码定位
    private String controller;
    private String handler;
    private String serviceMethod;

    // 异常
    private String errorCode;
    private String exceptionType;
    private String message;
    private String rootCause;
    private List<String> stackTop;

    // 请求负载
    private String clientIp;
    private String userAgent;
    private String requestBodyDigest;
    private String requestParams;
}
