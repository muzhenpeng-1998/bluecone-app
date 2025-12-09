package com.bluecone.app.resource.api.dto;

import java.time.Instant;
import java.util.Map;

/**
 * 上传策略视图，前端可直接使用返回的表单字段完成文件上传。
 *
 * @param uploadToken 幂等上传 token，完成回调时需校验
 * @param uploadUrl   目标上传地址
 * @param formFields  必需的表单字段
 * @param expiresAt   策略过期时间
 */
public record UploadPolicyView(String uploadToken,
                               String uploadUrl,
                               Map<String, String> formFields,
                               Instant expiresAt) {
}
