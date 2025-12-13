package com.bluecone.app.core.event.consume.api;

/**
 * 事件消费结果。
 *
 * @param replayed    是否重放（之前已成功处理）
 * @param inProgress  是否仍在处理中
 * @param succeeded   是否本次处理成功
 * @param retryCount  当前累计重试次数
 */
public record ConsumeResult(
        boolean replayed,
        boolean inProgress,
        boolean succeeded,
        int retryCount
) {
}

