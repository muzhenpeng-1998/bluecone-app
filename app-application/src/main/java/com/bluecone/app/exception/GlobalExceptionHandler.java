package com.bluecone.app.exception;

import com.bluecone.app.core.exception.ApiErrorResponse;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.core.log.error.ExceptionEvent;
import com.bluecone.app.core.log.error.ExceptionEventFactory;
import com.bluecone.app.core.log.error.ExceptionEventPipeline;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ExceptionEventFactory exceptionEventFactory;
    private final ExceptionEventPipeline exceptionEventPipeline;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        ApiErrorResponse response = ApiErrorResponse.of(ex.getCode(), ex.getMessage(), path);

        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.OK.value());
        exceptionEventPipeline.process(event);

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        ApiErrorResponse response = ApiErrorResponse.of(
                ErrorCode.INTERNAL_ERROR.getCode(),
                ErrorCode.INTERNAL_ERROR.getMessage(),
                path
        );

        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.INTERNAL_SERVER_ERROR.value());
        exceptionEventPipeline.process(event);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
