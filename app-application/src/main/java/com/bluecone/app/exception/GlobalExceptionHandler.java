package com.bluecone.app.exception;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.error.ParamErrorCode;
import com.bluecone.app.core.error.PublicIdErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.log.error.ExceptionEvent;
import com.bluecone.app.core.log.error.ExceptionEventFactory;
import com.bluecone.app.core.log.error.ExceptionEventPipeline;
import com.bluecone.app.core.idresolve.api.PublicIdInvalidException;
import com.bluecone.app.core.idresolve.api.PublicIdNotFoundException;
import com.bluecone.app.core.publicid.exception.PublicIdForbiddenException;
import com.bluecone.app.core.publicid.exception.PublicIdLookupMissingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

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
     * 处理参数校验异常 - @Valid 校验失败。
     * <p>返回 HTTP 400 + ApiResponse.fail("INVALID_PARAM", "字段名: 错误原因")</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, 
            HttpServletRequest request) {
        
        // 提取所有字段错误并拼接为可读消息
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        
        ApiResponse<Void> response = ApiResponse.fail(
                ParamErrorCode.INVALID_PARAM.getCode(),
                errorMessage.isEmpty() ? ParamErrorCode.INVALID_PARAM.getMessage() : errorMessage
        );
        
        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.BAD_REQUEST.value());
        exceptionEventPipeline.process(event);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理参数绑定异常 - 参数类型不匹配等。
     * <p>返回 HTTP 400 + ApiResponse.fail("INVALID_PARAM", "字段名: 错误原因")</p>
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(
            BindException ex, 
            HttpServletRequest request) {
        
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        
        ApiResponse<Void> response = ApiResponse.fail(
                ParamErrorCode.INVALID_PARAM.getCode(),
                errorMessage.isEmpty() ? ParamErrorCode.INVALID_PARAM.getMessage() : errorMessage
        );
        
        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.BAD_REQUEST.value());
        exceptionEventPipeline.process(event);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理约束违反异常 - @Validated 方法级校验失败。
     * <p>返回 HTTP 400 + ApiResponse.fail("INVALID_PARAM", "字段路径: 错误原因")</p>
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex, 
            HttpServletRequest request) {
        
        String errorMessage = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        
        ApiResponse<Void> response = ApiResponse.fail(
                ParamErrorCode.INVALID_PARAM.getCode(),
                errorMessage.isEmpty() ? ParamErrorCode.INVALID_PARAM.getMessage() : errorMessage
        );
        
        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.BAD_REQUEST.value());
        exceptionEventPipeline.process(event);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理未知异常 - 内部错误。
     * <p>返回 HTTP 500 + ApiResponse.fail("SYS-500-000", "系统异常，请稍后重试")</p>
     * <p>不泄露堆栈信息给前端。</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex, HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.fail(
                CommonErrorCode.SYSTEM_ERROR.getCode(),
                CommonErrorCode.SYSTEM_ERROR.getMessage()
        );

        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.INTERNAL_SERVER_ERROR.value());
        exceptionEventPipeline.process(event);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 处理 PublicIdInvalidException (idresolve) - Public ID 无效。
     * <p>返回 HTTP 400 + ApiResponse.fail("PUBLIC_ID_INVALID", message)</p>
     */
    @ExceptionHandler(PublicIdInvalidException.class)
    public ResponseEntity<ApiResponse<Void>> handlePublicIdInvalid(PublicIdInvalidException ex,
                                                                  HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.fail(
                PublicIdErrorCode.PUBLIC_ID_INVALID.getCode(),
                ex.getMessage()
        );
        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.BAD_REQUEST.value());
        exceptionEventPipeline.process(event);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理 PublicIdNotFoundException (idresolve) - Public ID 未找到。
     * <p>返回 HTTP 404 + ApiResponse.fail("PUBLIC_ID_NOT_FOUND", message)</p>
     */
    @ExceptionHandler(PublicIdNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handlePublicIdNotFound(PublicIdNotFoundException ex,
                                                                   HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.fail(
                PublicIdErrorCode.PUBLIC_ID_NOT_FOUND.getCode(),
                ex.getMessage()
        );
        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.NOT_FOUND.value());
        exceptionEventPipeline.process(event);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 处理 PublicIdInvalidException (governance) - Public ID 无效。
     * <p>返回 HTTP 400 + ApiResponse.fail("PUBLIC_ID_INVALID", message)</p>
     */
    @ExceptionHandler(com.bluecone.app.core.publicid.exception.PublicIdInvalidException.class)
    public ResponseEntity<ApiResponse<Void>> handleGovernancePublicIdInvalid(
            com.bluecone.app.core.publicid.exception.PublicIdInvalidException ex,
            HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.fail(
                PublicIdErrorCode.PUBLIC_ID_INVALID.getCode(), 
                ex.getMessage()
        );
        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.BAD_REQUEST.value());
        exceptionEventPipeline.process(event);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理 PublicIdNotFoundException (governance) - Public ID 未找到。
     * <p>返回 HTTP 404 + ApiResponse.fail("PUBLIC_ID_NOT_FOUND", message)</p>
     */
    @ExceptionHandler(com.bluecone.app.core.publicid.exception.PublicIdNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleGovernancePublicIdNotFound(
            com.bluecone.app.core.publicid.exception.PublicIdNotFoundException ex,
            HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.fail(
                PublicIdErrorCode.PUBLIC_ID_NOT_FOUND.getCode(), 
                ex.getMessage()
        );
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
        ApiResponse<Void> response = ApiResponse.fail(
                PublicIdErrorCode.PUBLIC_ID_FORBIDDEN.getCode(), 
                ex.getMessage()
        );
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
        ApiResponse<Void> response = ApiResponse.fail(
                PublicIdErrorCode.PUBLIC_ID_LOOKUP_MISSING.getCode(), 
                ex.getMessage()
        );
        ExceptionEvent event = exceptionEventFactory.fromException(ex, request, HttpStatus.INTERNAL_SERVER_ERROR.value());
        exceptionEventPipeline.process(event);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
