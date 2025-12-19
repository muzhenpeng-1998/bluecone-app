package com.bluecone.app.growth.api.enums;

/**
 * 归因状态枚举
 */
public enum AttributionStatus {
    /**
     * 待确认（已绑定，等待首单）
     */
    PENDING,
    
    /**
     * 已确认（完成首单）
     */
    CONFIRMED,
    
    /**
     * 已失效
     */
    INVALID
}
