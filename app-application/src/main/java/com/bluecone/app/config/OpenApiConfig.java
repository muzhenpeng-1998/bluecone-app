package com.bluecone.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * Springdoc OpenAPI 配置 - 多分组 API 文档。
 *
 * <p>分组策略（5 个独立分组）：</p>
 * <ul>
 *   <li><b>Mini</b>：小程序用户侧接口（com.bluecone.app.api.mini / /api/mini/**）</li>
 *   <li><b>Merchant</b>：商户侧管理接口（com.bluecone.app.api.merchant / /api/merchant/**）</li>
 *   <li><b>Admin</b>：平台运营后台接口（com.bluecone.app.api.admin / /api/admin/**）</li>
 *   <li><b>Open</b>：回调/开放接口（com.bluecone.app.api.open / /api/open/**），用于支付/微信/webhook 等</li>
 *   <li><b>Internal</b>：内部/调试接口（com.bluecone.app.api.internal / /api/internal/**），仅 dev/local 启用</li>
 * </ul>
 *
 * <p>认证策略：</p>
 * <ul>
 *   <li>Mini/Merchant/Admin 组：默认要求 Bearer JWT 认证</li>
 *   <li>Open 组：不要求 Bearer（通常使用签名校验）</li>
 *   <li>Internal 组：仅 dev/local 环境启用</li>
 * </ul>
 *
 * <p>自定义请求头：</p>
 * <ul>
 *   <li>X-Tenant-Id：租户标识（可选，根据业务需要）</li>
 *   <li>X-Store-Id：门店标识（可选，根据业务需要）</li>
 *   <li>X-Request-Id：请求追踪 ID（可选）</li>
 * </ul>
 *
 * <p>环境策略：</p>
 * <ul>
 *   <li>dev/local：所有分组启用，包括 Internal</li>
 *   <li>test/staging：所有分组启用，但 Internal 可选</li>
 *   <li>prod：仅启用 Mini/Merchant/Admin/Open，Internal 禁用</li>
 * </ul>
 *
 * <p>访问地址（默认）：</p>
 * <ul>
 *   <li>Swagger UI：http://localhost:8080/swagger-ui/index.html</li>
 *   <li>OpenAPI JSON：http://localhost:8080/v3/api-docs</li>
 *   <li>分组 JSON：http://localhost:8080/v3/api-docs/mini</li>
 * </ul>
 *
 * @author BlueCone Team
 * @since 1.0.0
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * 全局 OpenAPI 配置。
     *
     * <p>包含：</p>
     * <ul>
     *   <li>API 基本信息（title/version/description/contact）</li>
     *   <li>安全认证方案（HTTP Bearer JWT）</li>
     *   <li>自定义请求头定义</li>
     * </ul>
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(apiServers())
                .components(new Components()
                        // 安全认证：HTTP Bearer JWT
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, bearerSecurityScheme())
                        // 自定义请求头
                        .addParameters("X-Tenant-Id", tenantIdParameter())
                        .addParameters("X-Store-Id", storeIdParameter())
                        .addParameters("X-Request-Id", requestIdParameter())
                );
    }

    /**
     * Mini 分组：小程序用户侧接口。
     *
     * <p>扫描范围：</p>
     * <ul>
     *   <li>包路径：com.bluecone.app.api.mini</li>
     *   <li>URL 前缀：/api/mini/**</li>
     * </ul>
     *
     * <p>认证要求：需要 Bearer JWT</p>
     *
     * <p>业务域（Tag 分类）：</p>
     * <ul>
     *   <li>Mini - Cart：购物车</li>
     *   <li>Mini - Order：订单</li>
     *   <li>Mini - Member：会员</li>
     *   <li>Mini - Product：商品</li>
     * </ul>
     */
    @Bean
    public GroupedOpenApi miniApi() {
        return GroupedOpenApi.builder()
                .group("mini")
                .displayName("Mini（小程序用户侧）")
                .pathsToMatch("/api/mini/**")
                .packagesToScan("com.bluecone.app.api.mini")
                .addOpenApiCustomizer(openApi -> {
                    openApi.info(new Info()
                            .title("BlueCone Mini API")
                            .description("小程序用户侧接口（购物车、订单、会员等）")
                            .version("1.0.0"));
                    openApi.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
                })
                .build();
    }

    /**
     * Merchant 分组：商户侧管理接口。
     *
     * <p>扫描范围：</p>
     * <ul>
     *   <li>包路径：com.bluecone.app.api.merchant</li>
     *   <li>URL 前缀：/api/merchant/**</li>
     * </ul>
     *
     * <p>认证要求：需要 Bearer JWT</p>
     *
     * <p>业务域（Tag 分类）：</p>
     * <ul>
     *   <li>Merchant - Product：商品管理</li>
     *   <li>Merchant - Store：门店管理</li>
     *   <li>Merchant - Order：订单管理</li>
     *   <li>Merchant - Member：会员管理</li>
     * </ul>
     */
    @Bean
    public GroupedOpenApi merchantApi() {
        return GroupedOpenApi.builder()
                .group("merchant")
                .displayName("Merchant（商户侧）")
                .pathsToMatch("/api/merchant/**")
                .packagesToScan("com.bluecone.app.api.merchant")
                .addOpenApiCustomizer(openApi -> {
                    openApi.info(new Info()
                            .title("BlueCone Merchant API")
                            .description("商户侧管理接口（商品、门店、订单等）")
                            .version("1.0.0"));
                    openApi.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
                })
                .build();
    }

    /**
     * Admin 分组：平台运营后台接口。
     *
     * <p>扫描范围：</p>
     * <ul>
     *   <li>包路径：com.bluecone.app.api.admin</li>
     *   <li>URL 前缀：/api/admin/**</li>
     * </ul>
     *
     * <p>认证要求：需要 Bearer JWT</p>
     *
     * <p>业务域（Tag 分类）：</p>
     * <ul>
     *   <li>Admin - Tenant：租户管理</li>
     *   <li>Admin - Store：门店管理</li>
     *   <li>Admin - Order：订单管理</li>
     *   <li>Admin - Member：会员管理</li>
     *   <li>Admin - Config：配置管理</li>
     *   <li>Admin - Notify：通知管理</li>
     * </ul>
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("Admin（平台运营后台）")
                .pathsToMatch("/api/admin/**")
                .packagesToScan("com.bluecone.app.api.admin")
                .addOpenApiCustomizer(openApi -> {
                    openApi.info(new Info()
                            .title("BlueCone Admin API")
                            .description("平台运营后台接口（租户、门店、配置等）")
                            .version("1.0.0"));
                    openApi.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
                })
                .build();
    }

    /**
     * Open 分组：回调/开放接口。
     *
     * <p>扫描范围：</p>
     * <ul>
     *   <li>包路径：com.bluecone.app.api.open</li>
     *   <li>URL 前缀：/api/open/**</li>
     * </ul>
     *
     * <p>认证要求：不要求 Bearer（通常使用签名校验）</p>
     *
     * <p>业务域（Tag 分类）：</p>
     * <ul>
     *   <li>Open - Payment：支付回调</li>
     *   <li>Open - WeChat：微信回调</li>
     *   <li>Open - Webhook：第三方 Webhook</li>
     *   <li>Open - Store：门店公开接口</li>
     * </ul>
     */
    @Bean
    public GroupedOpenApi openApi() {
        return GroupedOpenApi.builder()
                .group("open")
                .displayName("Open（回调/开放接口）")
                .pathsToMatch("/api/open/**")
                .packagesToScan("com.bluecone.app.api.open")
                .addOpenApiCustomizer(openApi -> {
                    openApi.info(new Info()
                            .title("BlueCone Open API")
                            .description("回调/开放接口（支付、微信、Webhook 等），通常使用签名校验而非 Bearer")
                            .version("1.0.0"));
                    // Open 分组不要求 Bearer 认证
                })
                .build();
    }

    /**
     * Internal 分组：内部/调试接口。
     *
     * <p>扫描范围：</p>
     * <ul>
     *   <li>包路径：com.bluecone.app.api.internal</li>
     *   <li>URL 前缀：/api/internal/**</li>
     * </ul>
     *
     * <p>启用策略：</p>
     * <ul>
     *   <li>仅在 dev/local profile 启用</li>
     *   <li>生产环境（prod）不启用</li>
     * </ul>
     *
     * <p>业务域（Tag 分类）：</p>
     * <ul>
     *   <li>Internal - Debug：调试工具</li>
     *   <li>Internal - Cache：缓存管理</li>
     *   <li>Internal - Health：健康检查</li>
     * </ul>
     *
     * <p>注：可以通过配置属性 {@code springdoc.api-docs.groups.internal.enabled=false} 在特定环境禁用。</p>
     */
    @Bean
    @Profile({"dev", "local"})
    @ConditionalOnProperty(name = "springdoc.api-docs.groups.internal.enabled", havingValue = "true", matchIfMissing = true)
    public GroupedOpenApi internalApi() {
        return GroupedOpenApi.builder()
                .group("internal")
                .displayName("Internal（内部/调试）")
                .pathsToMatch("/api/internal/**")
                .packagesToScan("com.bluecone.app.api.internal")
                .addOpenApiCustomizer(openApi -> {
                    openApi.info(new Info()
                            .title("BlueCone Internal API")
                            .description("内部/调试接口，仅 dev/local 环境启用，生产环境不可用")
                            .version("1.0.0"));
                    openApi.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
                })
                .build();
    }

    // ==================== 私有方法：配置细节 ====================

    /**
     * API 基本信息。
     */
    private Info apiInfo() {
        return new Info()
                .title("BlueCone Application API")
                .description("""
                        BlueCone 应用 API 文档
                        
                        本文档包含 5 个分组：
                        - **Mini**：小程序用户侧接口
                        - **Merchant**：商户侧管理接口
                        - **Admin**：平台运营后台接口
                        - **Open**：回调/开放接口
                        - **Internal**：内部/调试接口（仅 dev/local）
                        
                        认证方式：HTTP Bearer JWT（Mini/Merchant/Admin 组）
                        
                        自定义请求头：
                        - X-Tenant-Id：租户标识
                        - X-Store-Id：门店标识
                        - X-Request-Id：请求追踪 ID
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("BlueCone Team")
                        .email("dev@bluecone.com")
                        .url("https://bluecone.com"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://bluecone.com/license"));
    }

    /**
     * API 服务器列表（可选，用于多环境切换）。
     */
    private List<Server> apiServers() {
        return List.of(
                new Server().url("http://localhost:8080").description("本地开发环境"),
                new Server().url("https://api-dev.bluecone.com").description("开发环境"),
                new Server().url("https://api-test.bluecone.com").description("测试环境"),
                new Server().url("https://api.bluecone.com").description("生产环境")
        );
    }

    /**
     * Bearer 认证方案（HTTP Bearer JWT）。
     */
    private SecurityScheme bearerSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        JWT 认证方式
                        
                        使用方式：
                        1. 调用登录接口获取 accessToken
                        2. 在请求头中添加：Authorization: Bearer {accessToken}
                        
                        适用分组：Mini / Merchant / Admin / Internal
                        
                        注：Open 分组通常使用签名校验，不需要 Bearer Token
                        """);
    }

    /**
     * X-Tenant-Id 请求头参数定义。
     */
    private Parameter tenantIdParameter() {
        return new Parameter()
                .name("X-Tenant-Id")
                .in("header")
                .description("租户标识（可选，部分接口需要）。如果 JWT Token 中已包含 tenantId，则无需传递此 Header")
                .required(false)
                .schema(new io.swagger.v3.oas.models.media.Schema<String>()
                        .type("string")
                        .example("1001"));
    }

    /**
     * X-Store-Id 请求头参数定义。
     */
    private Parameter storeIdParameter() {
        return new Parameter()
                .name("X-Store-Id")
                .in("header")
                .description("门店标识（可选，部分接口需要）。如果 JWT Token 中已包含 storeId，则无需传递此 Header")
                .required(false)
                .schema(new io.swagger.v3.oas.models.media.Schema<String>()
                        .type("string")
                        .example("2001"));
    }

    /**
     * X-Request-Id 请求头参数定义。
     */
    private Parameter requestIdParameter() {
        return new Parameter()
                .name("X-Request-Id")
                .in("header")
                .description("请求追踪 ID（可选，用于日志追踪和问题排查）。建议客户端生成 UUID 并在重试时保持不变")
                .required(false)
                .schema(new io.swagger.v3.oas.models.media.Schema<String>()
                        .type("string")
                        .format("uuid")
                        .example("550e8400-e29b-41d4-a716-446655440000"));
    }
}
