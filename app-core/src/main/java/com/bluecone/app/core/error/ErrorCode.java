package com.bluecone.app.core.error;

/**
 * 统一错误码接口。
 * <p>所有业务模块的错误码枚举都需要实现该接口，便于统一处理。</p>
 */
public interface ErrorCode {

    /**
     * 错误码，要求全局唯一。
     * 推荐格式：模块前缀-HTTP状态语义-序号，例如：ST-404-001。
     */
    String getCode();

    /**
     * 默认错误文案，主要用于日志和兜底返回。
     */
    String getMessage();
}
