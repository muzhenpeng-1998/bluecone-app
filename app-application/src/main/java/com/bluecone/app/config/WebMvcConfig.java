package com.bluecone.app.config;

import com.bluecone.app.core.publicid.web.PublicIdGovernanceArgumentResolver;
import com.bluecone.app.infra.tenant.TenantWebInterceptor;
import com.bluecone.app.web.idresolve.PublicIdArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置类
 *
 * 功能：
 * - 注册 Web 拦截器
 * - 配置拦截器的执行顺序和拦截路径
 *
 * 当前已注册的拦截器：
 * 1. TenantWebInterceptor - 租户拦截器
 *    - 从请求头中提取租户 ID
 *    - 存储到 TenantContext（ThreadLocal）
 *    - 请求结束后清理 TenantContext
 *
 * 注意事项：
 * - 拦截器的执行顺序由 order() 方法控制，数字越小优先级越高
 * - TenantWebInterceptor 优先级设置为 1，确保在业务逻辑执行前设置租户 ID
 * - 拦截所有路径（/**），确保所有请求都经过租户识别
 *
 * @author BlueCone Architecture Team
 * @since 1.0.0
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantWebInterceptor tenantWebInterceptor;
    private final PublicIdArgumentResolver publicIdArgumentResolver;
    private final PublicIdGovernanceArgumentResolver publicIdGovernanceArgumentResolver;

    /**
     * 构造函数注入
     *
     * @param tenantWebInterceptor 租户 Web 拦截器
     */
    public WebMvcConfig(TenantWebInterceptor tenantWebInterceptor,
                        PublicIdArgumentResolver publicIdArgumentResolver,
                        PublicIdGovernanceArgumentResolver publicIdGovernanceArgumentResolver) {
        this.tenantWebInterceptor = tenantWebInterceptor;
        this.publicIdArgumentResolver = publicIdArgumentResolver;
        this.publicIdGovernanceArgumentResolver = publicIdGovernanceArgumentResolver;
    }

    /**
     * 添加拦截器
     *
     * 注册顺序：
     * 1. TenantWebInterceptor (order=1) - 租户识别，优先级最高
     * 2. 未来可添加其他拦截器（如认证、日志、限流等）
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册租户拦截器
        registry.addInterceptor(tenantWebInterceptor)
                .addPathPatterns("/**")  // 拦截所有路径
                .order(1);               // 优先级：1（数字越小优先级越高）

        // TODO: 未来可添加其他拦截器
        // 例如：
        // registry.addInterceptor(authInterceptor)
        //         .addPathPatterns("/**")
        //         .excludePathPatterns("/login", "/register")
        //         .order(2);
    }

    @Override
    public void addArgumentResolvers(java.util.List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(publicIdArgumentResolver);
        resolvers.add(publicIdGovernanceArgumentResolver);
    }
}
