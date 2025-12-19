package com.bluecone.app.api.advice;

import com.bluecone.app.core.api.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 统一 API 响应包装 Advice。
 * <p>自动将 Controller 返回值包装为 ApiResponse，实现统一响应格式。</p>
 * 
 * <h3>包装规则：</h3>
 * <ul>
 *   <li>已经是 ApiResponse 类型：直接返回，不重复包装</li>
 *   <li>ResponseEntity：不包装（由 Controller 自行控制）</li>
 *   <li>byte[] / Resource / InputStreamResource：不包装（文件下载等）</li>
 *   <li>标记 @NoApiResponseWrap 注解的方法或类：不包装</li>
 *   <li>text/plain、application/xml、application/octet-stream 等非 JSON 响应：不包装</li>
 *   <li>回调类 Controller（包名含 callback 或路径含 /callback）：不包装</li>
 *   <li>其他情况：自动包装为 ApiResponse.ok(body)</li>
 * </ul>
 */
@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 检查方法或类是否标记了 @NoApiResponseWrap
        if (returnType.hasMethodAnnotation(NoApiResponseWrap.class) || 
            returnType.getDeclaringClass().isAnnotationPresent(NoApiResponseWrap.class)) {
            return false;
        }

        // 检查是否是回调 Controller（通过包名判断）
        String packageName = returnType.getDeclaringClass().getPackage().getName();
        if (packageName.contains(".callback") || packageName.contains(".webhook")) {
            return false;
        }

        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, 
                                   MethodParameter returnType, 
                                   MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType, 
                                   ServerHttpRequest request, 
                                   ServerHttpResponse response) {
        
        // 已经是 ApiResponse，直接返回
        if (body instanceof ApiResponse) {
            return body;
        }

        // ResponseEntity 不包装（由 Controller 自行控制）
        if (body instanceof ResponseEntity) {
            return body;
        }

        // 二进制数据不包装
        if (body instanceof byte[] || body instanceof Resource || body instanceof InputStreamResource) {
            return body;
        }

        // 非 JSON 响应不包装（text/plain、application/xml、application/octet-stream 等）
        if (selectedContentType != null && !selectedContentType.includes(MediaType.APPLICATION_JSON)) {
            return body;
        }

        // 路径包含 /callback 或 /notify 的不包装
        String path = request.getURI().getPath();
        if (path != null && (path.contains("/callback") || path.contains("/notify"))) {
            return body;
        }

        // 其他情况自动包装
        if (body == null) {
            return ApiResponse.ok();
        }
        return ApiResponse.ok(body);
    }
}
