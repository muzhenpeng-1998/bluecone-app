package com.bluecone.app.core.log.service;

import com.bluecone.app.core.log.ApiEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * Service 方法成功事件，沿用 ApiEvent 管道输出，专注服务层性能与调用上下文。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class ServiceEvent extends ApiEvent {

    private String serviceClass;
    private String serviceMethod;
    private String argsDigest;
    private String resultDigest;
    private Long elapsedMs;
    private String outcome; // SUCCESS / FAILED
}
