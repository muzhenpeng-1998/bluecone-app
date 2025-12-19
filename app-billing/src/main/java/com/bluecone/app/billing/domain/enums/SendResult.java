package com.bluecone.app.billing.domain.enums;

/**
 * 发送结果枚举
 */
public enum SendResult {
    
    /**
     * 发送成功
     */
    SUCCESS("SUCCESS", "发送成功"),
    
    /**
     * 发送失败
     */
    FAILED("FAILED", "发送失败");
    
    private final String code;
    private final String name;
    
    SendResult(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public static SendResult fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (SendResult result : values()) {
            if (result.code.equals(code)) {
                return result;
            }
        }
        return null;
    }
    
    public boolean isSuccess() {
        return this == SUCCESS;
    }
    
    public boolean isFailed() {
        return this == FAILED;
    }
}
