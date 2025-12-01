package com.bluecone.app.core.notify;

import java.util.Objects;
import java.util.UUID;

/**
 * 通知请求的同步响应（API 层）。
 *
 * <p>表示通知是否被平台接受入队（不代表最终送达）。</p>
 */
public class NotificationResponse {

    private final String requestId;
    private final boolean accepted;
    private final String reason;

    private NotificationResponse(String requestId, boolean accepted, String reason) {
        this.requestId = Objects.requireNonNullElseGet(requestId, () -> UUID.randomUUID().toString());
        this.accepted = accepted;
        this.reason = reason;
    }

    public static NotificationResponse accepted(String requestId) {
        return new NotificationResponse(requestId, true, null);
    }

    public static NotificationResponse rejected(String requestId, String reason) {
        return new NotificationResponse(requestId, false, reason);
    }

    public String getRequestId() {
        return requestId;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getReason() {
        return reason;
    }
}
