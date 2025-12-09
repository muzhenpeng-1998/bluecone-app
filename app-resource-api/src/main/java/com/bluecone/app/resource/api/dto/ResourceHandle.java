package com.bluecone.app.resource.api.dto;

import com.bluecone.app.resource.api.enums.ResourceProfileCode;

/**
 * 资源对外的只读句柄，包含绑定对象 ID 及访问信息。
 *
 * @param objectId    资源对象唯一 ID
 * @param profileCode 资源配置档位
 * @param url         资源访问地址
 * @param sizeBytes   资源大小（字节）
 * @param contentType 资源 MIME 类型
 */
public record ResourceHandle(String objectId,
                             ResourceProfileCode profileCode,
                             String url,
                             long sizeBytes,
                             String contentType) {
}
