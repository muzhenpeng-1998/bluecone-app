package com.bluecone.app.order.domain.enums;

import java.util.Arrays;

/**
 * 退款单状态。
 * 
 * <h3>状态流转规则：</h3>
 * <ul>
 *   <li>INIT（初始化）：退款单刚创建，尚未发起退款请求</li>
 *   <li>PROCESSING（处理中）：已向支付网关发起退款请求，等待结果</li>
 *   <li>SUCCESS（成功）：退款成功（收到支付网关回调通知）</li>
 *   <li>FAILED（失败）：退款失败（支付网关拒绝或超时）</li>
 * </ul>
 * 
 * <h3>终态判断：</h3>
 * <ul>
 *   <li>SUCCESS 和 FAILED 为终态，不可再流转</li>
 *   <li>INIT 和 PROCESSING 为非终态，可重试或更新</li>
 * </ul>
 */
public enum RefundStatus {
    
    /**
     * 初始化：退款单刚创建，尚未发起退款请求。
     */
    INIT("INIT", "初始化"),
    
    /**
     * 处理中：已向支付网关发起退款请求，等待结果。
     */
    PROCESSING("PROCESSING", "处理中"),
    
    /**
     * 成功：退款成功（收到支付网关回调通知）。
     */
    SUCCESS("SUCCESS", "成功"),
    
    /**
     * 失败：退款失败（支付网关拒绝或超时）。
     */
    FAILED("FAILED", "失败");
    
    private final String code;
    private final String desc;
    
    RefundStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDesc() {
        return desc;
    }
    
    /**
     * 根据 code 查找状态枚举。
     * 
     * @param code 状态码
     * @return 状态枚举，未找到返回 null
     */
    public static RefundStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 判断退款单是否处于终态（不可再流转的最终状态）。
     * 
     * @return true 表示终态（SUCCESS/FAILED）
     */
    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED;
    }
    
    /**
     * 判断退款单是否成功。
     * 
     * @return true 表示退款成功
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }
    
    /**
     * 判断退款单是否失败。
     * 
     * @return true 表示退款失败
     */
    public boolean isFailed() {
        return this == FAILED;
    }
    
    /**
     * 判断退款单是否处于处理中（可重试）。
     * 
     * @return true 表示处理中或初始化状态
     */
    public boolean isProcessing() {
        return this == INIT || this == PROCESSING;
    }
}
