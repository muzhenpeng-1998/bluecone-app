package com.bluecone.app.resource.domain.event;

/**
 * 资源删除事件，提示后续可做清理或审计记录。
 */
public class ResourceDeletedEvent {

    private final String resourceId;

    public ResourceDeletedEvent(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceId() {
        return resourceId;
    }
}
