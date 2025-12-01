package com.bluecone.app.infra.notify.delivery;

/**
 * 通道投递结果（Delivery 层）。
 */
public class DeliveryResult {

    private final boolean success;
    private final String errorCode;
    private final String errorMessage;

    private DeliveryResult(boolean success, String errorCode, String errorMessage) {
        this.success = success;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public static DeliveryResult success() {
        return new DeliveryResult(true, null, null);
    }

    public static DeliveryResult failure(String errorCode, String errorMessage) {
        return new DeliveryResult(false, errorCode, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
