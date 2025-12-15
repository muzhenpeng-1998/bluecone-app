package com.bluecone.app.core.publicid.web;

import com.bluecone.app.id.api.ResourceType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记 Controller 参数需要从 publicId 自动解析为内部主键或 ResolvedPublicId。
 * 
 * <p>支持的参数类型：</p>
 * <ul>
 *   <li>Long / long：注入解析后的主键（推荐，性能最优）</li>
 *   <li>ResolvedPublicId：注入完整解析结果（包含 type/publicId/tenantId/pk）</li>
 * </ul>
 * 
 * <p>使用示例：</p>
 * <pre>
 * // 示例 1：注入 Long 主键
 * &#64;GetMapping("/stores/{storeId}")
 * public StoreView detail(&#64;PathVariable &#64;ResolvePublicId(type=STORE) Long storePk) {
 *     // storePk 已自动解析为内部主键
 *     return storeService.getDetail(storePk);
 * }
 * 
 * // 示例 2：注入完整解析结果
 * &#64;GetMapping("/products/{productId}")
 * public ProductView detail(&#64;PathVariable &#64;ResolvePublicId(type=PRODUCT) ResolvedPublicId resolved) {
 *     Long productPk = resolved.asLong();
 *     String publicId = resolved.publicId();  // 用于日志/审计
 *     return productService.getDetail(productPk);
 * }
 * 
 * // 示例 3：可选参数
 * &#64;GetMapping("/stores")
 * public List&lt;StoreView&gt; list(&#64;RequestParam(required=false) &#64;ResolvePublicId(type=STORE, required=false) Long storePk) {
 *     // storePk 可能为 null
 *     return storeService.list(storePk);
 * }
 * </pre>
 * 
 * <p>配合 Scope Guard 使用：</p>
 * <pre>
 * // 启用 scope 校验（默认）
 * &#64;GetMapping("/stores/{storeId}")
 * public StoreView detail(&#64;PathVariable &#64;ResolvePublicId(type=STORE, scopeCheck=true) Long storePk) {
 *     // 自动校验：resolved.tenantId == context.tenantId
 *     // 自动校验：resolved.storePk == context.storePk（如 type=STORE）
 *     return storeService.getDetail(storePk);
 * }
 * 
 * // 禁用 scope 校验（谨慎使用，仅限管理端）
 * &#64;GetMapping("/admin/stores/{storeId}")
 * public StoreView adminDetail(&#64;PathVariable &#64;ResolvePublicId(type=STORE, scopeCheck=false) Long storePk) {
 *     // 跳过 scope 校验，允许跨租户/门店访问
 *     return storeService.getDetail(storePk);
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResolvePublicId {

    /**
     * 资源类型，用于校验 publicId 前缀和路由到对应的 Lookup。
     * 
     * @return 资源类型枚举
     */
    ResourceType type();

    /**
     * 是否必填，默认 true。
     * 
     * <p>如果为 true 且 publicId 为空，则抛出 400 Bad Request。</p>
     * <p>如果为 false 且 publicId 为空，则注入 null。</p>
     * 
     * @return true 表示必填，false 表示可选
     */
    boolean required() default true;

    /**
     * 是否启用 Scope Guard 校验，默认 true。
     * 
     * <p>启用时会自动校验：</p>
     * <ul>
     *   <li>租户隔离：resolved.tenantId == context.tenantId</li>
     *   <li>门店隔离（STORE 资源）：resolved.storePk == context.storePk</li>
     * </ul>
     * 
     * <p>禁用场景：</p>
     * <ul>
     *   <li>管理端接口：需要跨租户/门店访问</li>
     *   <li>平台侧接口：已通过 ApiSide=PLATFORM 跳过校验</li>
     * </ul>
     * 
     * @return true 表示启用，false 表示禁用
     */
    boolean scopeCheck() default true;
}

