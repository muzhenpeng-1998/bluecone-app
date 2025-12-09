package com.bluecone.app.resource.api.dto;

import com.bluecone.app.resource.api.enums.ResourceOwnerType;
import com.bluecone.app.resource.api.enums.ResourceProfileCode;

/**
 * 资源上传请求参数，包含目标业务实体与文件描述。
 *
 * @param profileCode 资源档位
 * @param ownerType   资源归属类型
 * @param ownerId     资源绑定的业务实体 ID
 * @param fileName    原始文件名
 * @param contentType 文件 MIME 类型
 * @param sizeBytes   文件大小，单位字节
 * @param hashSha256  文件 SHA-256 校验值
 */
public record ResourceUploadRequest(ResourceProfileCode profileCode,
                                    ResourceOwnerType ownerType,
                                    Long ownerId,
                                    String fileName,
                                    String contentType,
                                    long sizeBytes,
                                    String hashSha256) {
}
