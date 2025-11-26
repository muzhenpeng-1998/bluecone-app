package com.bluecone.app.core.api;

/**
 * API 版本化 Handler 统一接口
 * 所有版本的 Handler 必须实现此接口
 *
 * @author BlueCone Architecture Team
 */
public interface ApiHandler {

    /**
     * 处理 API 请求
     *
     * @param request 封装后的请求对象
     * @return 业务响应对象（由框架序列化为 JSON）
     * @throws Exception 业务异常由 GlobalExceptionHandler 统一处理
     */
    Object handle(ApiRequest request) throws Exception;
}
