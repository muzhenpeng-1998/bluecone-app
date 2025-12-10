package com.bluecone.app.api.onboarding.dto;

/**
 * 门店基础信息响应。
 */
public class StoreBasicInfoResponse {

    /**
     * 门店主键 ID。
     */
    private Long storeId;

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }
}

