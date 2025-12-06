package com.bluecone.app.infra.integration.domain;

/**
 * Integration Hub 投递任务状态机。
 *
 * <p>状态流转：
 * NEW → SENDING → SUCCESS
 * NEW → SENDING → FAILED（等待重试）
 * 重试超限或不可恢复 → DEAD
 * </p>
 */
public enum IntegrationDeliveryStatus {
    NEW,
    SENDING,
    SUCCESS,
    FAILED,
    DEAD
}
