package com.bluecone.app.exception;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.core.exception.BusinessException;
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

/**
 * 全局异常处理器。
 * <p>统一将异常转换为 ApiResponse 响应格式，确保成功/失败都使用相同的 envelope。</p>
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ExceptionEventFactory exceptionEventFactory;
    private final ExceptionEventPipeline exceptionEventPipeline;

    /**
     * 处理 BusinessException - 业务异常。
     * <p>返回 HTTP 200 + ApiResponse.fail(code, message)</p>
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.fail(ex.getCode(), ex.getMessage());

        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.OK.value());
        exceptionEventPipeline.process(event);

        return ResponseEntity.ok(response);
    }

    /**
     * 处理未知异常 - 内部错误。
     * <p>返回 HTTP 500 + ApiResponse.fail("INTERNAL_ERROR", "internal error")</p>
     * <p>不泄露堆栈信息给前端。</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex, HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.fail(
                ErrorCode.INTERNAL_ERROR.getCode(),
                ErrorCode.INTERNAL_ERROR.getMessage()
        );

        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.INTERNAL_SERVER_ERROR.value());
        exceptionEventPipeline.process(event);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 处理 PublicIdInvalidException - Public ID 无效。
     * <p>返回 HTTP 400 + ApiResponse.fail("INVALID_PARAM", message)</p>
     */
    @ExceptionHandler(PublicIdInvalidException.class)
    public ResponseEntity<ApiResponse<Void>> handlePublicIdInvalid(PublicIdInvalidException ex,
                                                                  HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.fail(
                ErrorCode.INVALID_PARAM.getCode(),
                ex.getMessage()
        );
        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.BAD_REQUEST.value());
        exceptionEventPipeline.process(event);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理 PublicIdNotFoundException - Public ID 未找到。
     * <p>返回 HTTP 404 + ApiResponse.fail("NOT_FOUND", message)</p>
     */
    @ExceptionHandler(PublicIdNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handlePublicIdNotFound(PublicIdNotFoundException ex,
                                                                   HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.fail(
                ErrorCode.NOT_FOUND.getCode(),
                ex.getMessage()
        );
        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.NOT_FOUND.value());
        exceptionEventPipeline.process(event);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 处理 Public ID Governance 相关异常 - Public ID 无效。
     * <p>返回 HTTP 400 + ApiResponse.fail("PUBLIC_ID_INVALID", message)</p>
     */
    @ExceptionHandler(com.bluecone.app.core.publicid.exception.PublicIdInvalidException.class)
    public ResponseEntity<ApiResponse<Void>> handleGovernancePublicIdInvalid(
            com.bluecone.app.core.publicid.exception.PublicIdInvalidException ex,
            HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.fail("PUBLIC_ID_INVALID", ex.getMessage());
        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.BAD_REQUEST.value());
        exceptionEventPipeline.process(event);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理 Public ID Governance 相关异常 - Public ID 未找到。
     * <p>返回 HTTP 404 + ApiResponse.fail("PUBLIC_ID_NOT_FOUND", message)</p>
     */
    @ExceptionHandler(com.bluecone.app.core.publicid.exception.PublicIdNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleGovernancePublicIdNotFound(
            com.bluecone.app.core.publicid.exception.PublicIdNotFoundException ex,
            HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.fail("PUBLIC_ID_NOT_FOUND", ex.getMessage());
        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.NOT_FOUND.value());
        exceptionEventPipeline.process(event);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 处理 PublicIdForbiddenException - Public ID 访问被禁止。
     * <p>返回 HTTP 403 + ApiResponse.fail("PUBLIC_ID_FORBIDDEN", message)</p>
     */
    @ExceptionHandler(PublicIdForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handlePublicIdForbidden(PublicIdForbiddenException ex,
                                                                    HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.fail("PUBLIC_ID_FORBIDDEN", ex.getMessage());
        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.FORBIDDEN.value());
        exceptionEventPipeline.process(event);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * 处理 PublicIdLookupMissingException - Public ID 查找配置缺失。
     * <p>返回 HTTP 500 + ApiResponse.fail("PUBLIC_ID_LOOKUP_MISSING", message)</p>
     */
    @ExceptionHandler(PublicIdLookupMissingException.class)
    public ResponseEntity<ApiResponse<Void>> handlePublicIdLookupMissing(PublicIdLookupMissingException ex,
                                                                        HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.fail("PUBLIC_ID_LOOKUP_MISSING", ex.getMessage());
        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.INTERNAL_SERVER_ERROR.value());
        exceptionEventPipeline.process(event);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
