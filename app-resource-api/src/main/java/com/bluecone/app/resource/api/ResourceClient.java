package com.bluecone.app.resource.api;

import com.bluecone.app.resource.api.dto.BindResourceCommand;
import com.bluecone.app.resource.api.dto.ResourceHandle;
import com.bluecone.app.resource.api.dto.ResourceQuery;
import com.bluecone.app.resource.api.dto.ResourceUploadRequest;
import com.bluecone.app.resource.api.dto.UploadPolicyView;
import com.bluecone.app.resource.api.dto.UnbindResourceCommand;
import com.bluecone.app.resource.api.enums.ResourceOwnerType;
import com.bluecone.app.resource.api.enums.ResourcePurpose;

import java.util.List;

/**
 * 资源中心对外暴露的主入口，提供资源上传、绑定以及查询等统一能力。
 */
public interface ResourceClient {

    /**
     * 请求生成上传策略，供客户端直接上传文件或交由存储回调使用。
     *
     * @param request 上传请求参数
     * @return 有效的上传策略视图，用于后续上传调用
     */
    UploadPolicyView requestUploadPolicy(ResourceUploadRequest request);

    /**
     * 上传完成后由业务调用，将存储层返回的 storageKey 与业务资源建立关联，完成落库。
     *
     * @param uploadToken 完成上传的唯一幂等 token
     * @param storageKey  存储文件标识
     * @param sizeBytes   文件大小（字节）
     * @param hashSha256  文件哈希
     * @return 绑定后的资源句柄
     */
    ResourceHandle completeUpload(String uploadToken, String storageKey, long sizeBytes, String hashSha256);

    /**
     * 获取某个业务实体在指定用途下的主资源。
     *
     * @param ownerType  资源所属类型
     * @param ownerId    业务实体 ID
     * @param purpose    资源用途
     * @return 主资源句柄，如不存在可抛出 {@link com.bluecone.app.resource.api.exception.ResourceNotFoundException}
     */
    ResourceHandle getMainResource(ResourceOwnerType ownerType, Long ownerId, ResourcePurpose purpose);

    /**
     * 列出某个业务实体在指定用途下的全部资源列表。
     *
     * @param query 查询参数
     * @return 资源句柄列表
     */
    List<ResourceHandle> listResources(ResourceQuery query);

    /**
     * 将已有资源对象绑定至业务实体。
     *
     * @param command 绑定命令
     */
    void bindExistingObject(BindResourceCommand command);

    /**
     * 解绑业务实体与资源对象之间的关联关系。
     *
     * @param command 解绑命令
     */
    void unbindResource(UnbindResourceCommand command);
}
