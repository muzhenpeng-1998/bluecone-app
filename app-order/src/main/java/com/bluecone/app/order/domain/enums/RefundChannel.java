package com.bluecone.app.order.domain.enums;

import java.util.Arrays;

/**
 * 退款渠道。
 * 
 * <h3>支持的渠道：</h3>
 * <ul>
 *   <li>WECHAT：微信支付退款</li>
 *   <li>ALIPAY：支付宝退款</li>
 *   <li>MOCK：Mock 退款（测试用）</li>
 * </ul>
 */
public enum RefundChannel {
    
    /**
     * 微信支付退款。
     */
    WECHAT("WECHAT", "微信支付"),
    
    /**
     * 支付宝退款。
     */
    ALIPAY("ALIPAY", "支付宝"),
    
    /**
     * Mock 退款（测试用）。
     */
    MOCK("MOCK", "Mock退款");
    
    private final String code;
    private final String desc;
    
    RefundChannel(String code, String desc) {
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
     * 根据 code 查找渠道枚举。
     * 
     * @param code 渠道码
     * @return 渠道枚举，未找到返回 null
     */
    public static RefundChannel fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
