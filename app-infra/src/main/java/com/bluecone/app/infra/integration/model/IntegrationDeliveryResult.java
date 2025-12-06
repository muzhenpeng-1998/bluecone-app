package com.bluecone.app.infra.integration.model;

/**
 * 通道投递结果。
 */
public class IntegrationDeliveryResult {

    private final boolean success;
    private final Integer httpStatus;
    private final String errorCode;
    private final String errorMessage;
    private final Long durationMs;

    private IntegrationDeliveryResult(final boolean success,
                                      final Integer httpStatus,
                                      final String errorCode,
                                      final String errorMessage,
                                      final Long durationMs) {
        this.success = success;
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
    }

    public static IntegrationDeliveryResult success(final Integer httpStatus, final long durationMs) {
        return new IntegrationDeliveryResult(true, httpStatus, null, null, durationMs);
    }

    public static IntegrationDeliveryResult failure(final String errorCode,
                                                    final String errorMessage,
                                                    final Integer httpStatus,
                                                    final long durationMs) {
        return new IntegrationDeliveryResult(false, httpStatus, errorCode, errorMessage, durationMs);
    }

    public boolean isSuccess() {
        return success;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Long getDurationMs() {
        return durationMs;
    }
}
