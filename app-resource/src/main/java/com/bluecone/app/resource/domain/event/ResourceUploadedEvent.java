package com.bluecone.app.resource.domain.event;

/**
 * 资源上传完成事件，后续可用于触发异步处理或通知。
 */
public class ResourceUploadedEvent {

    private final String resourceId;
    private final String storageKey;

    public ResourceUploadedEvent(String resourceId, String storageKey) {
        this.resourceId = resourceId;
        this.storageKey = storageKey;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getStorageKey() {
        return storageKey;
    }
}
