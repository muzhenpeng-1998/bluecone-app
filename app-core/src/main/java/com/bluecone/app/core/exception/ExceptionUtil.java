package com.bluecone.app.core.exception;

public class ExceptionUtil {

    public static String format(Exception e) {
        if (e == null) {
            return "Unknown error";
        }
        return e.getClass().getSimpleName() + ": " + safeMessage(e);
    }

    public static Throwable rootCause(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    public static String safeMessage(Exception e) {
        if (e == null) {
            return "Unknown error";
        }
        String message = e.getMessage();
        return message != null ? message : e.getClass().getSimpleName();
    }

    public static BusinessException toBusinessException(Exception e) {
        if (e instanceof BusinessException) {
            return (BusinessException) e;
        }
        return BusinessException.of(
                ErrorCode.INTERNAL_ERROR.getCode(),
                safeMessage(e)
        );
    }
}
