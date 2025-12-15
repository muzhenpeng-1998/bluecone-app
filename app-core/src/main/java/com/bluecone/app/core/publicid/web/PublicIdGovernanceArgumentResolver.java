package com.bluecone.app.core.publicid.web;

import com.bluecone.app.core.apicontract.ApiSide;
import com.bluecone.app.core.gateway.ApiContext;
import com.bluecone.app.core.gateway.ApiContextHolder;
import com.bluecone.app.core.publicid.api.PublicIdGovernanceResolver;
import com.bluecone.app.core.publicid.api.ResolvedPublicId;
import com.bluecone.app.core.publicid.exception.PublicIdInvalidException;
import com.bluecone.app.core.publicid.guard.ScopeGuard;
import com.bluecone.app.core.publicid.guard.ScopeGuardContext;
import com.bluecone.app.core.store.StoreContext;
import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.id.api.ResourceType;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/**
 * Public ID Governance 参数解析器，支持将 publicId 自动解析为 Long 主键或 ResolvedPublicId。
 * 
 * <p>核心流程：</p>
 * <ol>
 *   <li>提取 publicId：从 PathVariable 或 RequestParam 获取</li>
 *   <li>解析租户：从 ApiContext 或 TenantContext 获取 tenantId</li>
 *   <li>调用 Resolver：PublicIdGovernanceResolver.resolve()</li>
 *   <li>Scope 校验：如启用 scopeCheck，调用 ScopeGuard.check()</li>
 *   <li>返回结果：根据参数类型返回 Long 或 ResolvedPublicId</li>
 * </ol>
 * 
 * <p>与现有 PublicIdArgumentResolver 的区别：</p>
 * <ul>
 *   <li>现有：基于映射表 + 缓存，返回 Ulid128</li>
 *   <li>新版：直接查业务表，返回 Long 主键，支持 Scope Guard</li>
 * </ul>
 */
@Component
public class PublicIdGovernanceArgumentResolver implements HandlerMethodArgumentResolver {

    private static final Logger log = LoggerFactory.getLogger(PublicIdGovernanceArgumentResolver.class);

    private final PublicIdGovernanceResolver governanceResolver;
    private final ScopeGuard scopeGuard;

    public PublicIdGovernanceArgumentResolver(PublicIdGovernanceResolver governanceResolver,
                                              ScopeGuard scopeGuard) {
        this.governanceResolver = governanceResolver;
        this.scopeGuard = scopeGuard;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        ResolvePublicId ann = parameter.getParameterAnnotation(ResolvePublicId.class);
        if (ann == null) {
            return false;
        }
        Class<?> type = parameter.getParameterType();
        return Long.class.equals(type) || long.class.equals(type) || ResolvedPublicId.class.equals(type);
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

        // 1. 提取 publicId
        String paramName = resolveParamName(parameter);
        String publicId = extractPublicId(request, paramName);

        // 2. 处理可选参数
        if (!StringUtils.hasText(publicId)) {
            if (ann.required()) {
                throw new PublicIdInvalidException(
                        String.format("参数 %s 不能为空（resourceType=%s）", paramName, ann.type()));
            }
            return null;  // 可选参数且为空，返回 null
        }

        // 3. 解析租户 ID
        long tenantId = resolveTenantId();

        // 4. 调用 Governance Resolver 解析 publicId
        ResourceType resourceType = ann.type();
        ResolvedPublicId resolved = governanceResolver.resolve(tenantId, resourceType, publicId);

        // 5. Scope Guard 校验
        if (ann.scopeCheck()) {
            ScopeGuardContext guardContext = buildScopeGuardContext(tenantId);
            scopeGuard.check(resolved, guardContext);
        }

        // 6. 根据参数类型返回结果
        Class<?> paramType = parameter.getParameterType();
        if (Long.class.equals(paramType) || long.class.equals(paramType)) {
            return resolved.asLong();
        }
        if (ResolvedPublicId.class.equals(paramType)) {
            return resolved;
        }

        throw new IllegalStateException("不支持的参数类型：" + paramType);
    }

    /**
     * 解析参数名称（优先取注解值，否则取参数名）。
     */
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

    /**
     * 提取 publicId（优先 PathVariable，其次 RequestParam）。
     */
    private String extractPublicId(HttpServletRequest request, String paramName) {
        // 1. 尝试从 PathVariable 提取
        @SuppressWarnings("unchecked")
        Map<String, String> pathVars = (Map<String, String>) request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (pathVars != null && paramName != null) {
            String v = pathVars.get(paramName);
            if (v != null) {
                return v;
            }
        }

        // 2. 尝试从 RequestParam 提取
        if (paramName != null) {
            String v = request.getParameter(paramName);
            if (v != null) {
                return v;
            }
        }

        return null;
    }

    /**
     * 解析租户 ID（优先 ApiContext，其次 TenantContext）。
     */
    private long resolveTenantId() {
        // 1. 尝试从 ApiContext 获取
        ApiContext apiCtx = ApiContextHolder.get();
        if (apiCtx != null && apiCtx.getTenantId() != null) {
            try {
                return Long.parseLong(apiCtx.getTenantId());
            } catch (NumberFormatException ex) {
                log.warn("ApiContext.tenantId 格式非法：{}", apiCtx.getTenantId());
            }
        }

        // 2. 尝试从 TenantContext 获取
        String tenantIdStr = TenantContext.getTenantId();
        if (StringUtils.hasText(tenantIdStr)) {
            try {
                return Long.parseLong(tenantIdStr);
            } catch (NumberFormatException ex) {
                log.warn("TenantContext.tenantId 格式非法：{}", tenantIdStr);
            }
        }

        throw new PublicIdInvalidException("租户未登录或上下文缺失");
    }

    /**
     * 构造 Scope Guard 上下文。
     */
    private ScopeGuardContext buildScopeGuardContext(long tenantId) {
        ApiContext apiCtx = ApiContextHolder.get();
        if (apiCtx == null) {
            // 无 ApiContext，使用默认值（USER 侧，无门店）
            return new ScopeGuardContext(tenantId, null, ApiSide.USER);
        }

        // 提取 ApiSide
        ApiSide apiSide = apiCtx.getApiSide();
        if (apiSide == null) {
            apiSide = ApiSide.USER;  // 默认为 USER 侧
        }

        // 提取 storePk（从 STORE_CONTEXT 属性）
        Long storePk = null;
        Object storeCtxObj = apiCtx.getAttribute("STORE_CONTEXT");
        if (storeCtxObj instanceof StoreContext storeCtx) {
            StoreContext storeContext = storeCtx;
            if (storeContext.snapshot() != null && storeContext.snapshot().ext() != null) {
                Object storeIdObj = storeContext.snapshot().ext().get("storeId");
                if (storeIdObj instanceof Long) {
                    storePk = (Long) storeIdObj;
                }
            }
        }

        return new ScopeGuardContext(tenantId, storePk, apiSide);
    }
}

