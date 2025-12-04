package com.bluecone.app.exception;

import com.bluecone.app.api.ApiResponse;
import com.bluecone.app.core.exception.ApiErrorResponse;
import com.bluecone.app.core.exception.BizException;
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

    /**
     * 处理统一 BizException，返回标准 ApiResponse。
     */
    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBizException(BizException ex) {
        return ApiResponse.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        ApiErrorResponse response = ApiErrorResponse.of(
                com.bluecone.app.core.exception.ErrorCode.INTERNAL_ERROR.getCode(),
                com.bluecone.app.core.exception.ErrorCode.INTERNAL_ERROR.getMessage(),
                path
        );

        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.INTERNAL_SERVER_ERROR.value());
        exceptionEventPipeline.process(event);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
