package com.bluecone.app.promo.api.enums;

/**
 * 优惠券模板状态
 */
public enum TemplateStatus {
    
    /**
     * 草稿（编辑中，不可发券）
     */
    DRAFT,
    
    /**
     * 上线（可发券）
     */
    ONLINE,
    
    /**
     * 下线（停止发券）
     */
    OFFLINE
}
