package com.bluecone.app.core.api;

import org.slf4j.MDC;

import java.time.Instant;

/**
 * 统一API响应包装类
 * 
 * <p>用于所有对外API接口的响应封装，提供标准化的响应格式。
 * 
 * <h3>响应格式</h3>
 * <pre>{@code
 * {
 *   "code": "OK",                    // 业务状态码
 *   "message": "success",            // 提示信息
 *   "data": { ... },                 // 业务数据
 *   "traceId": "abc123",             // 追踪ID
 *   "timestamp": "2025-12-20T10:30:00Z"  // 响应时间戳
 * }
 * }</pre>
 * 
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 成功响应（带数据）
 * return ApiResponse.ok(userInfo);
 * 
 * // 成功响应（无数据）
 * return ApiResponse.ok();
 * 
 * // 失败响应
 * return ApiResponse.fail("USER_NOT_FOUND", "用户不存在");
 * }</pre>
 * 
 * <h3>状态码规范</h3>
 * <ul>
 *   <li><b>OK</b> - 请求成功</li>
 *   <li><b>SYS-xxx-xxx</b> - 系统级错误（如SYS-500-000表示系统异常）</li>
 *   <li><b>BIZ-xxx-xxx</b> - 业务级错误（如BIZ-404-001表示资源不存在）</li>
 *   <li><b>模块-xxx-xxx</b> - 模块级错误（如ORD-400-001表示订单参数错误）</li>
 * </ul>
 * 
 * <h3>设计原则</h3>
 * <ul>
 *   <li>HTTP状态码统一返回200，业务状态通过code字段区分</li>
 *   <li>前端只需判断code是否为"OK"即可知道业务是否成功</li>
 *   <li>message字段用于前端直接展示给用户</li>
 *   <li>traceId用于日志追踪和问题排查</li>
 * </ul>
 * 
 * <h3>与全局异常处理器配合</h3>
 * <p>业务代码抛出BusinessException时，全局异常处理器会自动转换为ApiResponse：</p>
 * <pre>{@code
 * // 业务代码
 * throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
 * 
 * // 自动转换为
 * {
 *   "code": "USER-404-001",
 *   "message": "用户不存在",
 *   "data": null,
 *   "traceId": "...",
 *   "timestamp": "..."
 * }
 * }</pre>
 *
 * @param <T> 响应数据类型
 * @author BlueCone
 * @since 1.0.0
 * @see com.bluecone.app.core.exception.BusinessException
 * @see com.bluecone.app.core.error.ErrorCode
 */
public class ApiResponse<T> {

    /**
     * 业务状态码
     * <ul>
     *   <li>OK - 成功</li>
     *   <li>其他 - 失败，格式为"模块-HTTP语义-序号"</li>
     * </ul>
     */
    private String code;
    
    /**
     * 提示信息
     * 面向用户的友好提示，可直接展示在前端
     */
    private String message;
    
    /**
     * 业务数据
     * 成功时返回具体的业务数据，失败时为null
     */
    private T data;
    
    /**
     * 追踪ID
     * 从MDC中获取，用于关联日志和问题排查
     */
    private String traceId;
    
    /**
     * 响应时间戳
     * 服务端生成响应的时间，UTC时区
     */
    private Instant timestamp;

    /**
     * 私有构造函数
     * 强制使用静态工厂方法创建实例
     * 
     * @param code 状态码
     * @param message 提示信息
     * @param data 业务数据
     */
    private ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = MDC.get("traceId");
        this.timestamp = Instant.now();
    }

    /**
     * 创建成功响应（带数据）
     * 
     * <p>用于返回业务数据的成功场景。
     * 
     * <p><b>使用示例：</b>
     * <pre>{@code
     * @GetMapping("/users/{id}")
     * public ApiResponse<UserVO> getUser(@PathVariable Long id) {
     *     UserVO user = userService.getById(id);
     *     return ApiResponse.ok(user);
     * }
     * }</pre>
     * 
     * @param <T> 数据类型
     * @param data 业务数据
     * @return 成功响应对象
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "success", data);
    }

    /**
     * 创建成功响应（无数据）
     * 
     * <p>用于不需要返回数据的成功场景，如删除、更新操作。
     * 
     * <p><b>使用示例：</b>
     * <pre>{@code
     * @DeleteMapping("/users/{id}")
     * public ApiResponse<Void> deleteUser(@PathVariable Long id) {
     *     userService.deleteById(id);
     *     return ApiResponse.ok();
     * }
     * }</pre>
     * 
     * @return 成功响应对象
     */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>("OK", "success", null);
    }

    /**
     * 创建失败响应
     * 
     * <p>用于业务失败场景，通常不直接调用，而是通过抛出BusinessException由全局异常处理器转换。
     * 
     * <p><b>使用示例：</b>
     * <pre>{@code
     * // 不推荐：直接返回失败响应
     * if (user == null) {
     *     return ApiResponse.fail("USER_NOT_FOUND", "用户不存在");
     * }
     * 
     * // 推荐：抛出异常，由全局异常处理器统一处理
     * if (user == null) {
     *     throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
     * }
     * }</pre>
     * 
     * @param <T> 数据类型
     * @param code 错误码
     * @param message 错误信息
     * @return 失败响应对象
     */
    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    // ========== 向后兼容别名 ==========
    
    /**
     * 成功响应（带数据）- 向后兼容别名
     * 
     * @deprecated 使用 {@link #ok(Object)} 替代
     * @param <T> 数据类型
     * @param data 业务数据
     * @return 成功响应对象
     */
    @Deprecated
    public static <T> ApiResponse<T> success(T data) {
        return ok(data);
    }

    /**
     * 成功响应（无数据）- 向后兼容别名
     * 
     * @deprecated 使用 {@link #ok()} 替代
     * @return 成功响应对象
     */
    @Deprecated
    public static ApiResponse<Void> success() {
        return ok();
    }

    // Getters

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
