package com.bluecone.app.core.exception;

import com.bluecone.app.core.error.ErrorCode;

/**
 * 统一业务异常。
 * <p>携带错误码与提示信息，便于全局异常处理器统一返回。</p>
 * 
 * <h3>推荐用法</h3>
 * <pre>{@code
 * // 推荐：使用 ErrorCode 枚举常量
 * throw new BusinessException(AuthErrorCode.AUTH_REQUIRED);
 * throw new BusinessException(ParamErrorCode.INVALID_PARAM, "用户ID不能为空");
 * throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND, "订单不存在", cause);
 * }</pre>
 * 
 * <h3>不推荐用法（已废弃）</h3>
 * <pre>{@code
 * // 不推荐：使用魔法字符串（已废弃，将在后续版本删除）
 * throw new BusinessException("AUTH_REQUIRED", "未登录");  // ❌ 废弃
 * throw BusinessException.of("INVALID_PARAM", "参数错误"); // ❌ 废弃
 * }</pre>
 * 
 * @see com.bluecone.app.core.error.ErrorCode
 * @see com.bluecone.app.core.error.AuthErrorCode
 * @see com.bluecone.app.core.error.ParamErrorCode
 * @see com.bluecone.app.core.error.CommonErrorCode
 */
public class BusinessException extends RuntimeException {

    private final String code;

    /**
     * 构造业务异常（使用 ErrorCode）。
     * 
     * <p>这是推荐的构造方式，使用结构化的错误码枚举常量。</p>
     * 
     * @param errorCode 错误码（来自 ErrorCode 枚举类）
     * 
     * @see com.bluecone.app.core.error.AuthErrorCode
     * @see com.bluecone.app.core.error.ParamErrorCode
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage(), null, false, false);
        this.code = errorCode.getCode();
    }

    /**
     * 构造业务异常（使用 ErrorCode + 自定义消息）。
     * 
     * <p>当默认错误消息不够具体时，可以使用此构造器提供更详细的上下文信息。</p>
     * 
     * @param errorCode 错误码（来自 ErrorCode 枚举类）
     * @param message 自定义错误消息（覆盖默认消息）
     * 
     * @see com.bluecone.app.core.error.AuthErrorCode
     * @see com.bluecone.app.core.error.ParamErrorCode
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message, null, false, false);
        this.code = errorCode.getCode();
    }

    /**
     * 构造业务异常（使用 ErrorCode + 自定义消息 + 原因）。
     * 
     * <p>用于包装底层异常，保留异常链，便于问题排查。</p>
     * 
     * @param errorCode 错误码（来自 ErrorCode 枚举类）
     * @param message 自定义错误消息
     * @param cause 原始异常
     * 
     * @see com.bluecone.app.core.error.AuthErrorCode
     * @see com.bluecone.app.core.error.ParamErrorCode
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause, false, false);
        this.code = errorCode.getCode();
    }

    /**
     * 构造业务异常（直接指定 code 和 message）。
     * 
     * <p><strong>⚠️ 已废弃</strong>：此构造器允许使用魔法字符串作为错误码，
     * 容易导致错误码碎片化、难以维护。</p>
     * 
     * <p>请改用 {@link #BusinessException(ErrorCode)} 或 {@link #BusinessException(ErrorCode, String)}，
     * 使用结构化的错误码枚举常量。</p>
     * 
     * <p>迁移示例：</p>
     * <pre>{@code
     * // 旧代码（不推荐）
     * throw new BusinessException("AUTH_REQUIRED", "未登录");
     * 
     * // 新代码（推荐）
     * throw new BusinessException(AuthErrorCode.AUTH_REQUIRED);
     * // 或者提供自定义消息
     * throw new BusinessException(AuthErrorCode.AUTH_REQUIRED, "会话已过期，请重新登录");
     * }</pre>
     * 
     * @param code 错误码字符串（不推荐使用魔法字符串）
     * @param message 错误消息
     * 
     * @deprecated 请使用 {@link #BusinessException(ErrorCode)} 替代，
     *             使用结构化的错误码枚举（如 {@link com.bluecone.app.core.error.AuthErrorCode}）。
     *             此构造器将在后续版本中删除。
     */
    @Deprecated
    public BusinessException(String code, String message) {
        super(message, null, false, false);
        this.code = code;
    }

    /**
     * 静态工厂方法（直接指定 code 和 message）。
     * 
     * <p><strong>⚠️ 已废弃</strong>：此方法允许使用魔法字符串作为错误码，
     * 容易导致错误码碎片化、难以维护。</p>
     * 
     * <p>请改用构造器 {@link #BusinessException(ErrorCode)} 或 {@link #BusinessException(ErrorCode, String)}。</p>
     * 
     * <p>迁移示例：</p>
     * <pre>{@code
     * // 旧代码（不推荐）
     * throw BusinessException.of("INVALID_PARAM", "参数错误");
     * 
     * // 新代码（推荐）
     * throw new BusinessException(ParamErrorCode.INVALID_PARAM, "参数错误");
     * }</pre>
     * 
     * @param code 错误码字符串（不推荐使用魔法字符串）
     * @param message 错误消息
     * @return BusinessException 实例
     * 
     * @deprecated 请使用 {@link #BusinessException(ErrorCode)} 构造器替代，
     *             使用结构化的错误码枚举（如 {@link com.bluecone.app.core.error.ParamErrorCode}）。
     *             此方法将在后续版本中删除。
     */
    @Deprecated
    public static BusinessException of(String code, String message) {
        return new BusinessException(code, message);
    }

    public String getCode() {
        return code;
    }
}
