package com.bluecone.app.core.publicid.exception;

import com.bluecone.app.id.api.ResourceType;

/**
 * 缺少 PublicIdLookup 实现异常。
 * 
 * <p>触发场景：</p>
 * <ul>
 *   <li>新增资源类型（如 INVENTORY）但未实现对应的 Lookup</li>
 *   <li>模块未正确注册 Lookup 到 Spring 容器</li>
 * </ul>
 * 
 * <p>HTTP 映射：500 Internal Server Error</p>
 * <p>错误码：PUBLIC_ID_LOOKUP_MISSING</p>
 * 
 * <p>解决方法：</p>
 * <ul>
 *   <li>在对应业务模块实现 PublicIdLookup 接口</li>
 *   <li>标记为 @Component 或在配置类中注册</li>
 *   <li>确保 Spring 扫描路径包含该实现类</li>
 * </ul>
 */
public class PublicIdLookupMissingException extends RuntimeException {

    private final ResourceType resourceType;

    public PublicIdLookupMissingException(ResourceType resourceType) {
        super(String.format("缺少资源类型 %s 的 PublicIdLookup 实现，请在对应模块实现该接口", resourceType));
        this.resourceType = resourceType;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }
}

