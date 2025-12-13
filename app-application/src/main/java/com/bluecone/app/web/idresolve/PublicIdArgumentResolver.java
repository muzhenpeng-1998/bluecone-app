package com.bluecone.app.web.idresolve;

import java.util.Map;
import java.util.Objects;

import com.bluecone.app.core.idresolve.api.PublicIdInvalidException;
import com.bluecone.app.core.idresolve.api.PublicIdNotFoundException;
import com.bluecone.app.core.idresolve.api.PublicIdResolver;
import com.bluecone.app.core.idresolve.api.ResolveKey;
import com.bluecone.app.core.idresolve.api.ResolvePublicId;
import com.bluecone.app.core.idresolve.api.ResolveResult;
import com.bluecone.app.core.idresolve.api.ResolvedId;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.infra.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 将请求中的 publicId 自动解析为内部 ULID128 的参数解析器。
 */
@Component
public class PublicIdArgumentResolver implements HandlerMethodArgumentResolver {

    private final PublicIdResolver publicIdResolver;

    public PublicIdArgumentResolver(PublicIdResolver publicIdResolver) {
        this.publicIdResolver = Objects.requireNonNull(publicIdResolver, "publicIdResolver");
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        ResolvePublicId ann = parameter.getParameterAnnotation(ResolvePublicId.class);
        if (ann == null) {
            return false;
        }
        Class<?> type = parameter.getParameterType();
        return Ulid128.class.equals(type) || ResolvedId.class.equals(type);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  @Nullable ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  @Nullable org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
        ResolvePublicId ann = parameter.getParameterAnnotation(ResolvePublicId.class);
        if (ann == null) {
            return null;
        }
        HttpServletRequest request = ((ServletWebRequest) webRequest).getRequest();
        String paramName = resolveParamName(parameter);
        String publicId = extractPublicId(request, paramName);
        long tenantId = resolveTenantId();

        ResolveResult result = publicIdResolver.resolve(new ResolveKey(tenantId, ann.type(), publicId));
        if (!result.hit() || !result.exists()) {
            String reason = result.reason();
            if (ResolveResult.REASON_INVALID_FORMAT.equals(reason)
                    || ResolveResult.REASON_PREFIX_MISMATCH.equals(reason)) {
                throw new PublicIdInvalidException("非法的 publicId：" + publicId);
            }
            throw new PublicIdNotFoundException("未找到 publicId 对应的资源：" + publicId);
        }

        if (Ulid128.class.equals(parameter.getParameterType())) {
            return result.internalId();
        }
        if (ResolvedId.class.equals(parameter.getParameterType())) {
            return new ResolvedId(result.internalId(), result.publicId());
        }
        throw new IllegalStateException("不支持的参数类型：" + parameter.getParameterType());
    }

    private long resolveTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null || tenantIdStr.isBlank()) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED, "租户未登录或上下文缺失");
        }
        try {
            return Long.parseLong(tenantIdStr);
        } catch (NumberFormatException ex) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "非法的租户标识");
        }
    }

    private String resolveParamName(MethodParameter parameter) {
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        if (requestParam != null && !requestParam.value().isBlank()) {
            return requestParam.value();
        }
        PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
        if (pathVariable != null && !pathVariable.value().isBlank()) {
            return pathVariable.value();
        }
        return parameter.getParameterName();
    }

    private String extractPublicId(HttpServletRequest request, String paramName) {
        @SuppressWarnings("unchecked")
        Map<String, String> pathVars = (Map<String, String>) request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (pathVars != null && paramName != null) {
            String v = pathVars.get(paramName);
            if (v != null) {
                return v;
            }
        }
        if (paramName != null) {
            String v = request.getParameter(paramName);
            if (v != null) {
                return v;
            }
        }
        return null;
    }
}

