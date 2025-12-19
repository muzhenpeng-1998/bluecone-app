package com.bluecone.app.order.domain.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单动作幂等日志聚合根。
 * 
 * <h3>设计目标：</h3>
 * <ul>
 *   <li>防止重复接单/拒单：同一 requestId 重复调用，返回已有结果，不产生副作用</li>
 *   <li>审计追溯：记录每次接单/拒单操作的操作人、时间、结果</li>
 *   <li>并发保护：配合订单乐观锁，确保并发安全</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderActionLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（自增）。
     */
    private Long id;

    /**
     * 租户ID。
     */
    private Long tenantId;

    /**
     * 门店ID。
     */
    private Long storeId;

    /**
     * 订单ID。
     */
    private Long orderId;

    /**
     * 动作类型：ACCEPT（接单）、REJECT（拒单）。
     */
    private String actionType;

    /**
     * 幂等唯一键：{tenantId}:{storeId}:{orderId}:{actionType}:{requestId}。
     * <p>同一 actionKey 只能执行一次，重复请求返回已有结果。</p>
     */
    private String actionKey;

    /**
     * 操作人ID。
     */
    private Long operatorId;

    /**
     * 操作人姓名（快照）。
     */
    private String operatorName;

    /**
     * 执行状态：PROCESSING、SUCCESS、FAILED。
     */
    @Builder.Default
    private String status = "PROCESSING";

    /**
     * 执行结果JSON（订单状态等）。
     */
    private String resultJson;

    /**
     * 错误码。
     */
    private String errorCode;

    /**
     * 错误消息。
     */
    private String errorMsg;

    /**
     * 扩展信息JSON（拒单原因等）。
     */
    @Builder.Default
    private Map<String, Object> ext = Collections.emptyMap();

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;

    /**
     * 生成 actionKey：{tenantId}:{storeId}:{orderId}:{actionType}:{requestId}。
     */
    public static String buildActionKey(Long tenantId, Long storeId, Long orderId, String actionType, String requestId) {
        return String.format("%d:%d:%d:%s:%s", tenantId, storeId, orderId, actionType, requestId);
    }

    /**
     * 标记为成功。
     */
    public void markSuccess(String resultJson) {
        this.status = "SUCCESS";
        this.resultJson = resultJson;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记为失败。
     */
    public void markFailed(String errorCode, String errorMsg) {
        this.status = "FAILED";
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 判断是否已成功。
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(this.status);
    }

    /**
     * 判断是否已失败。
     */
    public boolean isFailed() {
        return "FAILED".equals(this.status);
    }
}
