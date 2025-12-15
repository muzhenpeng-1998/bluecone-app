package com.bluecone.app.exception;

import com.bluecone.app.api.ApiResponse;
import com.bluecone.app.core.exception.ApiErrorResponse;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.core.log.error.ExceptionEvent;
import com.bluecone.app.core.log.error.ExceptionEventFactory;
import com.bluecone.app.core.log.error.ExceptionEventPipeline;
import com.bluecone.app.core.idresolve.api.PublicIdInvalidException;
import com.bluecone.app.core.idresolve.api.PublicIdNotFoundException;
import com.bluecone.app.core.publicid.exception.PublicIdForbiddenException;
import com.bluecone.app.core.publicid.exception.PublicIdLookupMissingException;
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

    @ExceptionHandler(PublicIdInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handlePublicIdInvalid(PublicIdInvalidException ex,
                                                                  HttpServletRequest request) {
        String path = request.getRequestURI();
        ApiErrorResponse response = ApiErrorResponse.of(
                ErrorCode.INVALID_PARAM.getCode(),
                ex.getMessage(),
                path
        );
        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.BAD_REQUEST.value());
        exceptionEventPipeline.process(event);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(PublicIdNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handlePublicIdNotFound(PublicIdNotFoundException ex,
                                                                   HttpServletRequest request) {
        String path = request.getRequestURI();
        ApiErrorResponse response = ApiErrorResponse.of(
                ErrorCode.NOT_FOUND.getCode(),
                ex.getMessage(),
                path
        );
        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.NOT_FOUND.value());
        exceptionEventPipeline.process(event);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 处理 Public ID Governance 相关异常（新增）
     */
    @ExceptionHandler(com.bluecone.app.core.publicid.exception.PublicIdInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handleGovernancePublicIdInvalid(
            com.bluecone.app.core.publicid.exception.PublicIdInvalidException ex,
            HttpServletRequest request) {
        String path = request.getRequestURI();
        ApiErrorResponse response = ApiErrorResponse.of(
                "PUBLIC_ID_INVALID",
                ex.getMessage(),
                path
        );
        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.BAD_REQUEST.value());
        exceptionEventPipeline.process(event);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(com.bluecone.app.core.publicid.exception.PublicIdNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleGovernancePublicIdNotFound(
            com.bluecone.app.core.publicid.exception.PublicIdNotFoundException ex,
            HttpServletRequest request) {
        String path = request.getRequestURI();
        ApiErrorResponse response = ApiErrorResponse.of(
                "PUBLIC_ID_NOT_FOUND",
                ex.getMessage(),
                path
        );
        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.NOT_FOUND.value());
        exceptionEventPipeline.process(event);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(PublicIdForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handlePublicIdForbidden(PublicIdForbiddenException ex,
                                                                    HttpServletRequest request) {
        String path = request.getRequestURI();
        ApiErrorResponse response = ApiErrorResponse.of(
                "PUBLIC_ID_FORBIDDEN",
                ex.getMessage(),
                path
        );
        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.FORBIDDEN.value());
        exceptionEventPipeline.process(event);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(PublicIdLookupMissingException.class)
    public ResponseEntity<ApiErrorResponse> handlePublicIdLookupMissing(PublicIdLookupMissingException ex,
                                                                        HttpServletRequest request) {
        String path = request.getRequestURI();
        ApiErrorResponse response = ApiErrorResponse.of(
                "PUBLIC_ID_LOOKUP_MISSING",
                ex.getMessage(),
                path
        );
        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.INTERNAL_SERVER_ERROR.value());
        exceptionEventPipeline.process(event);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
