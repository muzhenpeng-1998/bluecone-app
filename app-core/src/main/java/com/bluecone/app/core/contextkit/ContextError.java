package com.bluecone.app.core.contextkit;

/**
 * 通用上下文错误对象。
 *
 * @param code    错误类型
 * @param message 错误描述
 */
public record ContextError(ContextErrorCode code, String message) {
}

