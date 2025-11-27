package com.bluecone.app.core.log.service;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * Service 方法异常事件，避免堆栈外泄，仅保留关键信息。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class ServiceExceptionEvent extends ServiceEvent {

    private String exceptionType;
    private String exceptionMessage;
    private String rootCause;
    private String errorCode;
}
